package com.example.bookexchange.exchange.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.common.i18n.MessageKey;
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
        User sender = userUtil.createUser(600);
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
        assertHasVersion(content.get(0));
    }

    @Test
    void shouldReturnEmptyPage_whenUserHasNoCompletedExchangesInHistory() throws Exception {
        User user = userUtil.createUser(607);

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
        assertThat(exchange.getIsReadBySender()).isTrue();
    }

    @Test
    void shouldReturnUnreadUpdates_whenReceiverGetsNewExchangeRequest() throws Exception {
        ExchangeFixture fixture = createPendingExchange(620);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.UPDATES_PATH_UNREAD)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode content = body.path("data").path("content");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(content.get(0).path("userExchangeRole").asText()).isEqualTo("RECEIVER");
        assertThat(content.get(0).path("otherBookName").asText()).isNotBlank();
        assertThat(content.get(0).path("otherUserId").asLong()).isEqualTo(fixture.sender().getId());
        assertThat(content.get(0).path("otherUserNickname").asText()).isEqualTo(fixture.sender().getNickname());
        assertHasVersion(content.get(0));
    }

    @Test
    void shouldReturnUnreadUpdatesAgain_whenSenderDeclinesRequestAfterReceiverReadOffer() throws Exception {
        ExchangeFixture fixture = createPendingExchange(630);

        mockMvc.perform(get(ExchangePaths.OFFER_PATH_EXCHANGE_ID, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        clearPersistenceContext();

        Exchange readExchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        assertThat(readExchange.getIsReadByReceiver()).isTrue();

        MvcResult noUnreadResult = mockMvc.perform(get(ExchangePaths.UPDATES_PATH_UNREAD)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(responseBody(noUnreadResult).path("data").path("totalElements").asLong()).isZero();

        mockMvc.perform(patch(ExchangePaths.REQUEST_PATH_DECLINE_REQUEST, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .header(HttpHeaders.IF_MATCH, ifMatch(readExchange.getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        clearPersistenceContext();

        Exchange declinedExchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        assertThat(declinedExchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(declinedExchange.getIsReadBySender()).isTrue();
        assertThat(declinedExchange.getIsReadByReceiver()).isFalse();

        MvcResult unreadAgainResult = mockMvc.perform(get(ExchangePaths.UPDATES_PATH_UNREAD)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode unreadContent = responseBody(unreadAgainResult).path("data").path("content");

        assertThat(unreadContent).hasSize(1);
        assertThat(unreadContent.get(0).path("id").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(unreadContent.get(0).path("status").asText()).isEqualTo(ExchangeStatus.DECLINED.name());
        assertThat(unreadContent.get(0).path("userExchangeRole").asText()).isEqualTo("RECEIVER");
    }

    @Test
    void shouldReturnUnreadUpdatesToSender_whenReceiverDeclinesOffer() throws Exception {
        ExchangeFixture fixture = createPendingExchange(640);
        Exchange exchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();

        mockMvc.perform(patch(ExchangePaths.OFFER_PATH_DECLINE_OFFER, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .header(HttpHeaders.IF_MATCH, ifMatch(exchange.getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        clearPersistenceContext();

        Exchange declinedExchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        assertThat(declinedExchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(declinedExchange.getIsReadBySender()).isFalse();
        assertThat(declinedExchange.getIsReadByReceiver()).isTrue();

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.UPDATES_PATH_UNREAD)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = responseBody(mvcResult).path("data").path("content");

        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(content.get(0).path("status").asText()).isEqualTo(ExchangeStatus.DECLINED.name());
        assertThat(content.get(0).path("userExchangeRole").asText()).isEqualTo("SENDER");
        assertThat(content.get(0).path("otherUserId").asLong()).isEqualTo(fixture.receiver().getId());
        assertThat(content.get(0).path("otherUserNickname").asText()).isEqualTo(fixture.receiver().getNickname());
    }

    @Test
    void shouldReturnNotFound_whenUnrelatedUserGetsExchangeHistoryDetails() throws Exception {
        ExchangeFixture fixture = createCompletedExchange(612, ExchangeStatus.APPROVED);
        User unrelatedUser = userUtil.createUser(615);

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

    private ExchangeFixture createExchangeForSender(User sender, int base) {
        User receiver = userUtil.createUser(base);
        Long senderBookId = bookUtil.createBook(sender.getId(), base);
        Long receiverBookId = bookUtil.createBook(receiver.getId(), base + 1);
        Long exchangeId = exchangeUtilIT.createExchange(sender.getId(), receiver.getId(), senderBookId, receiverBookId);

        return new ExchangeFixture(
                sender,
                receiver,
                exchangeId,
                "Contact Details " + base,
                "Contact Details " + (base + 1)
        );
    }

    private ExchangeFixture createCompletedExchange(int base, ExchangeStatus status) {
        User sender = userUtil.createUser(base);
        User receiver = userUtil.createUser(base + 1);
        Long senderBookId = bookUtil.createBook(sender.getId(), base);
        Long receiverBookId = bookUtil.createBook(receiver.getId(), base + 1);
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
                "Contact Details " + base,
                "Contact Details " + (base + 1)
        );
    }

    private ExchangeFixture createPendingExchange(int base) {
        return createCompletedExchange(base, ExchangeStatus.PENDING);
    }

    private String historyPath(Long exchangeId) {
        return ExchangePaths.HISTORY_PATH + "/" + exchangeId;
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
