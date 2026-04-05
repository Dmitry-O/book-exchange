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
    void getExchangeHistoryReturnsOnlyCompletedExchanges() throws Exception {
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
    }

    @Test
    void getExchangeHistoryReturnsEmptyPageWhenThereAreNoCompletedExchanges() throws Exception {
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
    void getExchangeHistoryDetailsForSenderMarksExchangeAsRead() throws Exception {
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
        assertThat(body.path("data").path("contactDetails").asText()).isEqualTo(fixture.receiverContactDetails());
        assertThat(exchange.getIsReadBySender()).isTrue();
        assertThat(exchange.getIsReadByReceiver()).isFalse();
    }

    @Test
    void getExchangeHistoryDetailsForReceiverMarksExchangeAsRead() throws Exception {
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
        assertThat(body.path("data").path("contactDetails").asText()).isEqualTo(fixture.senderContactDetails());
        assertThat(exchange.getIsReadByReceiver()).isTrue();
        assertThat(exchange.getIsReadBySender()).isFalse();
    }

    @Test
    void getExchangeHistoryDetailsNotFoundForUnrelatedUser() throws Exception {
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
