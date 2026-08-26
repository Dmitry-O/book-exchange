package com.example.bookexchange.exchange.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.support.FixtureNumbers;
import com.example.bookexchange.support.TestBookStrings;
import com.example.bookexchange.updates.api.UpdatesPaths;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.support.fixture.BookFixtureSupport;
import com.example.bookexchange.support.fixture.ExchangeFixtureSupport;
import com.example.bookexchange.support.PageTestDefaults;
import com.example.bookexchange.support.fixture.UserFixtureSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class HistoryControllerIT extends IntegrationTestSupport {

    @Autowired
    ExchangeRepository exchangeRepository;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    UserFixtureSupport userUtil;

    @Autowired
    BookFixtureSupport bookUtil;

    @Autowired
    ExchangeFixtureSupport exchangeUtilIT;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldReturnCompletedExchanges_whenUserGetsExchangeHistory() throws Exception {
        User sender = userUtil.createUser(FixtureNumbers.history(600));
        ExchangeFixture approvedFixture = createExchangeForSender(sender, 601);
        ExchangeFixture declinedFixture = createExchangeForSender(sender, 603);
        ExchangeFixture pendingFixture = createExchangeForSender(sender, 605);

        Exchange approvedExchange = exchangeRepository.findById(approvedFixture.exchangeId()).orElseThrow();
        approvedExchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(approvedExchange);

        Exchange declinedExchange = exchangeRepository.findById(declinedFixture.exchangeId()).orElseThrow();
        declinedExchange.setStatus(ExchangeStatus.DECLINED);
        declinedExchange.setDeclinerUser(sender);
        exchangeRepository.save(declinedExchange);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.HISTORY_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode content = body.path("data").path("content");
        Set<Long> returnedIds = Set.of(
                content.get(0).path("id").asLong(),
                content.get(1).path("id").asLong()
        );

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(2);
        assertThat(content.size()).isEqualTo(2);
        assertThat(returnedIds).contains(approvedFixture.exchangeId(), declinedFixture.exchangeId());
        assertThat(returnedIds).doesNotContain(pendingFixture.exchangeId());
        assertThat(content.get(0).path("status").asText()).isIn(ExchangeStatus.APPROVED.name(), ExchangeStatus.DECLINED.name());
        assertThat(content.get(0).path("userNickname").asText()).isNotBlank();
        assertThat(content.get(0).path("otherUserId").asLong()).isPositive();
        assertThat(findExchangeById(content, approvedFixture.exchangeId()).path("userExchangeRole").asText()).isEqualTo("SENDER");
        assertThat(findExchangeById(content, declinedFixture.exchangeId()).path("userExchangeRole").asText()).isEqualTo("SENDER");
        assertHasVersion(content.get(0));
    }

    @Test
    void shouldReturnReceiverRole_whenReceiverGetsExchangeHistory() throws Exception {
        ExchangeFixture fixture = createCompletedExchange(606, ExchangeStatus.APPROVED);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.HISTORY_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = responseBody(mvcResult).path("data").path("content");
        JsonNode historyItem = findExchangeById(content, fixture.exchangeId());

        assertThat(content).hasSize(1);
        assertThat(historyItem.path("userExchangeRole").asText()).isEqualTo("RECEIVER");
        assertThat(historyItem.path("userNickname").asText()).isEqualTo(fixture.sender().getNickname());
        assertThat(historyItem.path("otherUserId").asLong()).isEqualTo(fixture.sender().getId());
    }

    @Test
    void shouldReturnEmptyPage_whenUserHasNoCompletedExchangesInHistory() throws Exception {
        User user = userUtil.createUser(FixtureNumbers.history(607));

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.HISTORY_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isZero();
        assertThat(body.path("data").path("content").size()).isZero();
    }

    @Test
    void shouldMarkExchangeAsRead_whenSenderGetsExchangeHistoryDetails() throws Exception {
        ExchangeFixture fixture = createCompletedExchange(608, ExchangeStatus.APPROVED);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.HISTORY_PATH_EXCHANGE_ID, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        clearPersistenceContext();

        Exchange exchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(body.path("data").path("status").asText()).isEqualTo(ExchangeStatus.APPROVED.name());
        assertThat(body.path("data").path("userExchangeRole").asText()).isEqualTo("SENDER");
        assertThat(body.path("data").path("userNickname").asText()).isEqualTo(fixture.receiver().getNickname());
        assertThat(body.path("data").path("otherUserId").asLong()).isEqualTo(fixture.receiver().getId());
        assertThat(body.path("data").path("contactDetails").asText()).isEqualTo(fixture.receiverContactDetails());
        assertVersion(body.path("data"), exchange.getVersion());
        assertThat(exchange.getIsReadBySender()).isTrue();
        assertThat(exchange.getIsReadByReceiver()).isFalse();
    }

    @Test
    void shouldMarkExchangeAsRead_whenReceiverGetsExchangeHistoryDetails() throws Exception {
        ExchangeFixture fixture = createCompletedExchange(610, ExchangeStatus.APPROVED);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.HISTORY_PATH_EXCHANGE_ID, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        clearPersistenceContext();

        Exchange exchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("userExchangeRole").asText()).isEqualTo("RECEIVER");
        assertThat(body.path("data").path("userNickname").asText()).isEqualTo(fixture.sender().getNickname());
        assertThat(body.path("data").path("otherUserId").asLong()).isEqualTo(fixture.sender().getId());
        assertThat(body.path("data").path("contactDetails").asText()).isEqualTo(fixture.senderContactDetails());
        assertVersion(body.path("data"), exchange.getVersion());
        assertThat(exchange.getIsReadByReceiver()).isTrue();
        assertThat(exchange.getIsReadBySender()).isFalse();
    }

    @Test
    void shouldKeepHistoryOrderStable_whenHistoryDetailsOpenedAndReadStateToggled() throws Exception {
        User sender = userUtil.createUser(FixtureNumbers.history(613));
        ExchangeFixture newerFixture = createExchangeForSender(sender, 614);
        ExchangeFixture olderFixture = createExchangeForSender(sender, 616);

        Exchange newerExchange = exchangeRepository.findById(newerFixture.exchangeId()).orElseThrow();
        newerExchange.setStatus(ExchangeStatus.APPROVED);
        newerExchange.setUpdateCreatedAt(Instant.parse("2026-04-24T10:00:00Z"));
        exchangeRepository.save(newerExchange);

        Exchange olderExchange = exchangeRepository.findById(olderFixture.exchangeId()).orElseThrow();
        olderExchange.setStatus(ExchangeStatus.APPROVED);
        olderExchange.setUpdateCreatedAt(Instant.parse("2026-04-23T10:00:00Z"));
        exchangeRepository.save(olderExchange);

        MvcResult initialHistoryResult = mockMvc.perform(get(ExchangePaths.HISTORY_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode initialContent = responseBody(initialHistoryResult).path("data").path("content");
        assertThat(initialContent.get(0).path("id").asLong()).isEqualTo(newerFixture.exchangeId());
        assertThat(initialContent.get(1).path("id").asLong()).isEqualTo(olderFixture.exchangeId());

        mockMvc.perform(get(ExchangePaths.HISTORY_PATH_EXCHANGE_ID, olderFixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(patch(UpdatesPaths.UPDATES_PATH_EXCHANGE_ID_READ_STATE, olderFixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("isRead", false)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        MvcResult stableHistoryResult = mockMvc.perform(get(ExchangePaths.HISTORY_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode stableContent = responseBody(stableHistoryResult).path("data").path("content");
        assertThat(stableContent.get(0).path("id").asLong()).isEqualTo(newerFixture.exchangeId());
        assertThat(stableContent.get(1).path("id").asLong()).isEqualTo(olderFixture.exchangeId());
    }

    @Test
    void shouldReturnNotFound_whenUnrelatedUserGetsExchangeHistoryDetails() throws Exception {
        ExchangeFixture fixture = createCompletedExchange(612, ExchangeStatus.APPROVED);
        User unrelatedUser = userUtil.createUser(FixtureNumbers.history(615));

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.HISTORY_PATH_EXCHANGE_ID, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(unrelatedUser))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.EXCHANGE_NOT_FOUND,
                historyPath(fixture.exchangeId())
        );
    }

    @Test
    void shouldReturnGiftHistoryDetailsWithNullSenderBook_whenReceiverGetsGiftExchangeHistoryDetails() throws Exception {
        User sender = userUtil.createUser(FixtureNumbers.history(623));
        User receiver = userUtil.createUser(FixtureNumbers.history(624));
        Long receiverBookId = bookUtil.createBook(receiver.getId(), FixtureNumbers.history(624));
        Exchange exchange = createGiftExchange(sender, receiver, receiverBookId);
        exchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(exchange);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.HISTORY_PATH_EXCHANGE_ID, exchange.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(receiver))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        clearPersistenceContext();

        Exchange persistedExchange = exchangeRepository.findById(exchange.getId()).orElseThrow();
        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("userExchangeRole").asText()).isEqualTo("RECEIVER");
        assertThat(body.path("data").path("userNickname").asText()).isEqualTo(sender.getNickname());
        assertThat(body.path("data").path("otherUserId").asLong()).isEqualTo(sender.getId());
        assertThat(body.path("data").path("senderBook").isNull()).isTrue();
        assertThat(body.path("data").path("contactDetails").isNull()).isTrue();
        assertVersion(body.path("data"), persistedExchange.getVersion());
        assertThat(persistedExchange.getIsReadByReceiver()).isTrue();
    }

    @Test
    void shouldHideContactDetails_whenSenderGetsDeclinedExchangeHistoryDetails() throws Exception {
        ExchangeFixture fixture = createCompletedExchange(625, ExchangeStatus.DECLINED);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.HISTORY_PATH_EXCHANGE_ID, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("status").asText()).isEqualTo(ExchangeStatus.DECLINED.name());
        assertThat(body.path("data").path("userExchangeRole").asText()).isEqualTo("SENDER");
        assertThat(body.path("data").path("contactDetails").isNull()).isTrue();
        assertThat(body.path("data").path("senderBook").path("contactDetails").isNull()).isTrue();
        assertThat(body.path("data").path("receiverBook").path("contactDetails").isNull()).isTrue();
    }

    @Test
    void shouldHideContactDetails_whenReceiverGetsDeclinedExchangeHistoryDetails() throws Exception {
        ExchangeFixture fixture = createCompletedExchange(627, ExchangeStatus.DECLINED);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.HISTORY_PATH_EXCHANGE_ID, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("status").asText()).isEqualTo(ExchangeStatus.DECLINED.name());
        assertThat(body.path("data").path("userExchangeRole").asText()).isEqualTo("RECEIVER");
        assertThat(body.path("data").path("contactDetails").isNull()).isTrue();
        assertThat(body.path("data").path("senderBook").path("contactDetails").isNull()).isTrue();
        assertThat(body.path("data").path("receiverBook").path("contactDetails").isNull()).isTrue();
    }

    private ExchangeFixture createExchangeForSender(User sender, int base) {
        int senderBookNumber = FixtureNumbers.history(base);
        int receiverNumber = FixtureNumbers.history(base + 1);
        User receiver = userUtil.createUser(receiverNumber);
        Long senderBookId = bookUtil.createBook(sender.getId(), senderBookNumber);
        Long receiverBookId = bookUtil.createBook(receiver.getId(), receiverNumber);
        Long exchangeId = exchangeUtilIT.createExchange(sender.getId(), receiver.getId(), senderBookId, receiverBookId);

        return new ExchangeFixture(
                sender,
                receiver,
                exchangeId,
                TestBookStrings.contactDetails(senderBookNumber),
                TestBookStrings.contactDetails(receiverNumber)
        );
    }

    private ExchangeFixture createCompletedExchange(int base, ExchangeStatus status) {
        int senderNumber = FixtureNumbers.history(base);
        int receiverNumber = FixtureNumbers.history(base + 1);
        User sender = userUtil.createUser(senderNumber);
        User receiver = userUtil.createUser(receiverNumber);
        Long senderBookId = bookUtil.createBook(sender.getId(), senderNumber);
        Long receiverBookId = bookUtil.createBook(receiver.getId(), receiverNumber);
        Long exchangeId = exchangeUtilIT.createExchange(sender.getId(), receiver.getId(), senderBookId, receiverBookId);
        Exchange exchange = exchangeRepository.findById(exchangeId).orElseThrow();

        exchange.setStatus(status);
        if (status == ExchangeStatus.DECLINED) {
            exchange.setDeclinerUser(sender);
        }
        exchangeRepository.save(exchange);

        return new ExchangeFixture(
                sender,
                receiver,
                exchangeId,
                TestBookStrings.contactDetails(senderNumber),
                TestBookStrings.contactDetails(receiverNumber)
        );
    }

    private JsonNode findExchangeById(JsonNode content, Long exchangeId) {
        for (JsonNode item : content) {
            if (item.path("id").asLong() == exchangeId) {
                return item;
            }
        }

        throw new IllegalStateException("Exchange with id=" + exchangeId + " was not found in response");
    }

    private String historyPath(Long exchangeId) {
        return ExchangePaths.HISTORY_PATH + "/" + exchangeId;
    }

    private Exchange createGiftExchange(User sender, User receiver, Long receiverBookId) {
        Book receiverBook = bookRepository.findById(receiverBookId).orElseThrow();
        receiverBook.setIsGift(true);
        bookRepository.save(receiverBook);

        Long exchangeId = exchangeUtilIT.createExchange(sender.getId(), receiver.getId(), null, receiverBookId);

        return exchangeRepository.findById(exchangeId).orElseThrow();
    }

    private record ExchangeFixture(
            User sender,
            User receiver,
            Long exchangeId,
            String senderContactDetails,
            String receiverContactDetails
    ) {
    }
}
