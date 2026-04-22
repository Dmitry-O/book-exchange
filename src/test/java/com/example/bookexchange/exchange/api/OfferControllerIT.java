package com.example.bookexchange.exchange.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.support.FixtureNumbers;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class OfferControllerIT extends IntegrationTestSupport {

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
    void shouldReturnPendingOffers_whenUserGetsOffers() throws Exception {
        ExchangeFixture pendingFixture = createPendingOffer(500);
        User secondSender = userUtil.createUser(FixtureNumbers.offer(502));
        Long secondSenderBookId = bookUtil.createBook(secondSender.getId(), FixtureNumbers.offer(502));
        Long approvedExchangeId = exchangeUtilIT.createExchange(
                secondSender.getId(),
                pendingFixture.receiver().getId(),
                secondSenderBookId,
                pendingFixture.receiverBookId()
        );

        Exchange approvedExchange = exchangeRepository.findById(approvedExchangeId).orElseThrow();
        approvedExchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(approvedExchange);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.OFFER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(pendingFixture.receiver()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode content = body.path("data").path("content");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(content.size()).isEqualTo(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(pendingFixture.exchangeId());
        assertThat(content.get(0).path("status").asText()).isEqualTo(ExchangeStatus.PENDING.name());
        assertThat(content.get(0).path("userNickname").asText()).isEqualTo(pendingFixture.sender().getNickname());
        assertThat(content.get(0).path("otherUserId").asLong()).isEqualTo(pendingFixture.sender().getId());
        assertHasVersion(content.get(0));
    }

    @Test
    void shouldReturnOfferDetails_whenReceiverGetsOfferDetails() throws Exception {
        ExchangeFixture fixture = createPendingOffer(503);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.OFFER_PATH_EXCHANGE_ID, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(body.path("data").path("status").asText()).isEqualTo(ExchangeStatus.PENDING.name());
        assertThat(body.path("data").path("userNickname").asText()).isEqualTo(fixture.sender().getNickname());
        assertThat(body.path("data").path("otherUserId").asLong()).isEqualTo(fixture.sender().getId());
    }

    @Test
    void shouldReturnGiftOfferInReceiverListWithNullSenderBookFields() throws Exception {
        User sender = userUtil.createUser(FixtureNumbers.offer(514));
        User receiver = userUtil.createUser(FixtureNumbers.offer(515));
        Long receiverBookId = bookUtil.createBook(receiver.getId(), FixtureNumbers.offer(515));
        Book receiverBook = bookRepository.findById(receiverBookId).orElseThrow();
        receiverBook.setIsGift(true);
        bookRepository.save(receiverBook);
        Long exchangeId = exchangeUtilIT.createExchange(sender.getId(), receiver.getId(), null, receiverBookId);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.OFFER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(receiver))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = responseBody(mvcResult).path("data").path("content");

        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(exchangeId);
        assertThat(content.get(0).path("status").asText()).isEqualTo(ExchangeStatus.PENDING.name());
        assertThat(content.get(0).path("userNickname").asText()).isEqualTo(sender.getNickname());
        assertThat(content.get(0).path("otherUserId").asLong()).isEqualTo(sender.getId());
        assertThat(content.get(0).get("senderBookName").isNull()).isTrue();
        assertThat(content.get(0).get("senderBookPhotoUrl").isNull()).isTrue();
    }

    @Test
    void shouldReturnNotFound_whenSenderGetsReceiverOfferDetails() throws Exception {
        ExchangeFixture fixture = createPendingOffer(504);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.OFFER_PATH_EXCHANGE_ID, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.EXCHANGE_NOT_FOUND,
                offerPath(fixture.exchangeId())
        );
    }

    @Test
    void shouldApproveOffer_whenReceiverApprovesPendingOffer() throws Exception {
        User receiver = userUtil.createUser(FixtureNumbers.offer(505));
        User firstSender = userUtil.createUser(FixtureNumbers.offer(506));
        User secondSender = userUtil.createUser(FixtureNumbers.offer(507));
        Long receiverBookId = bookUtil.createBook(receiver.getId(), FixtureNumbers.offer(505));
        Long firstSenderBookId = bookUtil.createBook(firstSender.getId(), FixtureNumbers.offer(506));
        Long secondSenderBookId = bookUtil.createBook(secondSender.getId(), FixtureNumbers.offer(507));
        Long approvedExchangeId = exchangeUtilIT.createExchange(
                firstSender.getId(),
                receiver.getId(),
                firstSenderBookId,
                receiverBookId
        );
        Long competingExchangeId = exchangeUtilIT.createExchange(
                secondSender.getId(),
                receiver.getId(),
                secondSenderBookId,
                receiverBookId
        );

        Exchange approvedExchange = exchangeRepository.findById(approvedExchangeId).orElseThrow();

        MvcResult mvcResult = mockMvc.perform(patch(ExchangePaths.OFFER_PATH_APPROVE_OFFER, approvedExchangeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(receiver))
                        .header(HttpHeaders.IF_MATCH, ifMatch(approvedExchange.getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        Exchange persistedApprovedExchange = exchangeRepository.findById(approvedExchangeId).orElseThrow();
        Exchange persistedCompetingExchange = exchangeRepository.findById(competingExchangeId).orElseThrow();
        Book persistedReceiverBook = bookRepository.findById(receiverBookId).orElseThrow();
        Book persistedSenderBook = bookRepository.findById(firstSenderBookId).orElseThrow();
        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("status").asText()).isEqualTo(ExchangeStatus.APPROVED.name());
        assertThat(persistedApprovedExchange.getStatus()).isEqualTo(ExchangeStatus.APPROVED);
        assertThat(persistedCompetingExchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(persistedReceiverBook.getIsExchanged()).isTrue();
        assertThat(persistedSenderBook.getIsExchanged()).isTrue();
        assertThat(eTagVersion(mvcResult)).isEqualTo(persistedApprovedExchange.getVersion());
    }

    @Test
    void shouldApproveGiftOfferWithoutSenderBook_whenReceiverApprovesGiftRequest() throws Exception {
        User sender = userUtil.createUser(FixtureNumbers.offer(512));
        User receiver = userUtil.createUser(FixtureNumbers.offer(513));
        Long receiverBookId = bookUtil.createBook(receiver.getId(), FixtureNumbers.offer(513));
        Book receiverBook = bookRepository.findById(receiverBookId).orElseThrow();
        receiverBook.setIsGift(true);
        bookRepository.save(receiverBook);
        Long approvedExchangeId = exchangeUtilIT.createExchange(sender.getId(), receiver.getId(), null, receiverBookId);
        Exchange approvedExchange = exchangeRepository.findById(approvedExchangeId).orElseThrow();

        MvcResult mvcResult = mockMvc.perform(patch(ExchangePaths.OFFER_PATH_APPROVE_OFFER, approvedExchangeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(receiver))
                        .header(HttpHeaders.IF_MATCH, ifMatch(approvedExchange.getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        Exchange persistedApprovedExchange = exchangeRepository.findById(approvedExchangeId).orElseThrow();
        Book persistedReceiverBook = bookRepository.findById(receiverBookId).orElseThrow();
        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("status").asText()).isEqualTo(ExchangeStatus.APPROVED.name());
        assertThat(body.path("data").path("senderBook").isNull()).isTrue();
        assertThat(persistedApprovedExchange.getStatus()).isEqualTo(ExchangeStatus.APPROVED);
        assertThat(persistedApprovedExchange.getSenderBook()).isNull();
        assertThat(persistedReceiverBook.getIsExchanged()).isTrue();
        assertThat(eTagVersion(mvcResult)).isEqualTo(persistedApprovedExchange.getVersion());
    }

    @Test
    void shouldReturnBadRequest_whenReceiverApprovesAlreadyApprovedOffer() throws Exception {
        ExchangeFixture fixture = createPendingOffer(508);
        Exchange exchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        exchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(exchange);
        clearPersistenceContext();

        Exchange approvedExchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();

        MvcResult mvcResult = mockMvc.perform(patch(ExchangePaths.OFFER_PATH_APPROVE_OFFER, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .header(HttpHeaders.IF_MATCH, ifMatch(approvedExchange.getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.EXCHANGE_CANT_BE_APPROVED,
                approvePath(fixture.exchangeId())
        );
    }

    @Test
    void shouldReturnConflict_whenReceiverApprovesOfferWithStaleVersion() throws Exception {
        ExchangeFixture fixture = createPendingOffer(509);

        MvcResult mvcResult = mockMvc.perform(patch(ExchangePaths.OFFER_PATH_APPROVE_OFFER, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .header(HttpHeaders.IF_MATCH, "\"999\"")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                409,
                MessageKey.SYSTEM_OPTIMISTIC_LOCK,
                approvePath(fixture.exchangeId())
        );
    }

    @Test
    void shouldDeclineOffer_whenReceiverDeclinesPendingOffer() throws Exception {
        ExchangeFixture fixture = createPendingOffer(510);
        Exchange exchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();

        MvcResult mvcResult = mockMvc.perform(patch(ExchangePaths.OFFER_PATH_DECLINE_OFFER, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .header(HttpHeaders.IF_MATCH, ifMatch(exchange.getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        Exchange declinedExchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("status").asText()).isEqualTo(ExchangeStatus.DECLINED.name());
        assertThat(declinedExchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(declinedExchange.getDeclinerUser().getId()).isEqualTo(fixture.receiver().getId());
        assertThat(mvcResult.getResponse().getHeader(HttpHeaders.ETAG)).isNotBlank();
    }

    @Test
    void shouldReturnBadRequest_whenReceiverDeclinesAlreadyApprovedOffer() throws Exception {
        ExchangeFixture fixture = createPendingOffer(511);
        Exchange exchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        exchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(exchange);
        clearPersistenceContext();

        Exchange approvedExchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();

        MvcResult mvcResult = mockMvc.perform(patch(ExchangePaths.OFFER_PATH_DECLINE_OFFER, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .header(HttpHeaders.IF_MATCH, ifMatch(approvedExchange.getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.EXCHANGE_CANT_BE_DECLINED,
                declinePath(fixture.exchangeId())
        );
    }

    private ExchangeFixture createPendingOffer(int base) {
        User sender = userUtil.createUser(base);
        User receiver = userUtil.createUser(base + 1);
        Long senderBookId = bookUtil.createBook(sender.getId(), base);
        Long receiverBookId = bookUtil.createBook(receiver.getId(), base + 1);
        Long exchangeId = exchangeUtilIT.createExchange(sender.getId(), receiver.getId(), senderBookId, receiverBookId);

        return new ExchangeFixture(sender, receiver, senderBookId, receiverBookId, exchangeId);
    }

    private String offerPath(Long exchangeId) {
        return ExchangePaths.OFFER_PATH + "/" + exchangeId;
    }

    private String approvePath(Long exchangeId) {
        return offerPath(exchangeId) + "/approve";
    }

    private String declinePath(Long exchangeId) {
        return offerPath(exchangeId) + "/decline";
    }

    private record ExchangeFixture(
            User sender,
            User receiver,
            Long senderBookId,
            Long receiverBookId,
            Long exchangeId
    ) {
    }
}
