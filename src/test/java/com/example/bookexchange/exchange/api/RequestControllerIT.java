package com.example.bookexchange.exchange.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.exchange.dto.RequestCreateDTO;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class RequestControllerIT extends IntegrationTestSupport {

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
    void shouldCreateRequest_whenPayloadIsValid() throws Exception {
        ExchangeFixture fixture = createExchangeFixture(400);
        RequestCreateDTO dto = buildRequestCreateDTO(fixture);

        MvcResult mvcResult = mockMvc.perform(post(ExchangePaths.REQUEST_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        Long exchangeId = body.path("data").path("id").asLong();
        Exchange exchange = exchangeRepository.findById(exchangeId).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("status").asText()).isEqualTo(ExchangeStatus.PENDING.name());
        assertThat(body.path("data").path("userNickname").asText()).isEqualTo(fixture.receiver().getNickname());
        assertThat(exchange.getSenderUser().getId()).isEqualTo(fixture.sender().getId());
        assertThat(exchange.getReceiverUser().getId()).isEqualTo(fixture.receiver().getId());
        assertThat(exchange.getSenderBook().getId()).isEqualTo(fixture.senderBookId());
        assertThat(exchange.getReceiverBook().getId()).isEqualTo(fixture.receiverBookId());
        assertThat(eTagVersion(mvcResult)).isEqualTo(exchange.getVersion());
    }

    @Test
    void shouldReturnBadRequest_whenCreateRequestPayloadIsInvalid() throws Exception {
        User sender = userUtil.createUser(401);

        MvcResult mvcResult = mockMvc.perform(post(ExchangePaths.REQUEST_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(sender))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RequestCreateDTO.builder().build())))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertValidationErrorResponse(responseBody(mvcResult), ExchangePaths.REQUEST_PATH);
    }

    @Test
    void shouldReturnBadRequest_whenUserCreatesRequestWithThemself() throws Exception {
        User user = userUtil.createUser(402);
        Long senderBookId = bookUtil.createBook(user.getId(), 402);
        Long receiverBookId = bookUtil.createBook(user.getId(), 403);
        RequestCreateDTO dto = RequestCreateDTO.builder()
                .receiverUserId(user.getId())
                .senderBookId(senderBookId)
                .receiverBookId(receiverBookId)
                .build();

        MvcResult mvcResult = mockMvc.perform(post(ExchangePaths.REQUEST_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.EXCHANGE_CANT_BE_WITH_YOURSELF,
                ExchangePaths.REQUEST_PATH
        );
    }

    @Test
    void shouldReturnNotFound_whenCreateRequestReceiverBookDoesNotExist() throws Exception {
        ExchangeFixture fixture = createExchangeFixture(404);
        RequestCreateDTO dto = RequestCreateDTO.builder()
                .receiverUserId(fixture.receiver().getId())
                .senderBookId(fixture.senderBookId())
                .receiverBookId(Long.MAX_VALUE)
                .build();

        MvcResult mvcResult = mockMvc.perform(post(ExchangePaths.REQUEST_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(responseBody(mvcResult), 404, MessageKey.BOOK_NOT_FOUND, ExchangePaths.REQUEST_PATH);
    }

    @Test
    void shouldReturnConflict_whenGiftBookAlreadyHasPendingRequest() throws Exception {
        ExchangeFixture fixture = createExchangeFixture(405);
        Book receiverBook = bookRepository.findById(fixture.receiverBookId()).orElseThrow();
        receiverBook.setIsGift(true);
        bookRepository.save(receiverBook);

        RequestCreateDTO dto = buildRequestCreateDTO(fixture);

        mockMvc.perform(post(ExchangePaths.REQUEST_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        MvcResult mvcResult = mockMvc.perform(post(ExchangePaths.REQUEST_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                409,
                MessageKey.BOOK_EXCHANGE_ALREADY_EXISTS,
                ExchangePaths.REQUEST_PATH
        );
    }

    @Test
    void shouldReturnRequestDetails_whenSenderGetsRequestDetails() throws Exception {
        ExchangeFixture fixture = createPendingExchange(406);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.REQUEST_PATH_EXCHANGE_ID, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(body.path("data").path("status").asText()).isEqualTo(ExchangeStatus.PENDING.name());
        assertThat(body.path("data").path("userNickname").asText()).isEqualTo(fixture.receiver().getNickname());
    }

    @Test
    void shouldReturnNotFound_whenReceiverGetsSenderRequestDetails() throws Exception {
        ExchangeFixture fixture = createPendingExchange(407);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.REQUEST_PATH_EXCHANGE_ID, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.EXCHANGE_NOT_FOUND,
                requestPath(fixture.exchangeId())
        );
    }

    @Test
    void shouldReturnPendingRequests_whenSenderGetsRequests() throws Exception {
        ExchangeFixture pendingFixture = createPendingExchange(408);
        User secondReceiver = userUtil.createUser(410);
        Long secondSenderBookId = bookUtil.createBook(pendingFixture.sender().getId(), 411);
        Long secondReceiverBookId = bookUtil.createBook(secondReceiver.getId(), 412);
        Long approvedExchangeId = exchangeUtilIT.createExchange(
                pendingFixture.sender().getId(),
                secondReceiver.getId(),
                secondSenderBookId,
                secondReceiverBookId
        );

        Exchange approvedExchange = exchangeRepository.findById(approvedExchangeId).orElseThrow();
        approvedExchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(approvedExchange);

        MvcResult mvcResult = mockMvc.perform(get(ExchangePaths.REQUEST_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(pendingFixture.sender()))
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
    }

    @Test
    void shouldDeclineRequest_whenSenderDeclinesPendingRequest() throws Exception {
        ExchangeFixture fixture = createPendingExchange(412);
        Exchange exchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();

        MvcResult mvcResult = mockMvc.perform(patch(ExchangePaths.REQUEST_PATH_DECLINE_REQUEST, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
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
        assertThat(declinedExchange.getDeclinerUser().getId()).isEqualTo(fixture.sender().getId());
        assertThat(mvcResult.getResponse().getHeader(HttpHeaders.ETAG)).isNotBlank();
    }

    @Test
    void shouldReturnNotFound_whenReceiverDeclinesSenderRequest() throws Exception {
        ExchangeFixture fixture = createPendingExchange(413);
        Exchange exchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();

        MvcResult mvcResult = mockMvc.perform(patch(ExchangePaths.REQUEST_PATH_DECLINE_REQUEST, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .header(HttpHeaders.IF_MATCH, ifMatch(exchange.getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.EXCHANGE_NOT_FOUND,
                declinePath(fixture.exchangeId())
        );
    }

    @Test
    void shouldReturnBadRequest_whenSenderDeclinesAlreadyApprovedRequest() throws Exception {
        ExchangeFixture fixture = createPendingExchange(414);
        Exchange exchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        exchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(exchange);
        clearPersistenceContext();

        Exchange approvedExchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();

        MvcResult mvcResult = mockMvc.perform(patch(ExchangePaths.REQUEST_PATH_DECLINE_REQUEST, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
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

    @Test
    void shouldReturnConflict_whenSenderDeclinesRequestWithStaleVersion() throws Exception {
        ExchangeFixture fixture = createPendingExchange(415);

        MvcResult mvcResult = mockMvc.perform(patch(ExchangePaths.REQUEST_PATH_DECLINE_REQUEST, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .header(HttpHeaders.IF_MATCH, "\"999\"")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                409,
                MessageKey.SYSTEM_OPTIMISTIC_LOCK,
                declinePath(fixture.exchangeId())
        );
    }

    private ExchangeFixture createExchangeFixture(int base) {
        User sender = userUtil.createUser(base);
        User receiver = userUtil.createUser(base + 1);
        Long senderBookId = bookUtil.createBook(sender.getId(), base);
        Long receiverBookId = bookUtil.createBook(receiver.getId(), base + 1);

        return new ExchangeFixture(sender, receiver, senderBookId, receiverBookId, null);
    }

    private ExchangeFixture createPendingExchange(int base) {
        ExchangeFixture fixture = createExchangeFixture(base);
        Long exchangeId = exchangeUtilIT.createExchange(
                fixture.sender().getId(),
                fixture.receiver().getId(),
                fixture.senderBookId(),
                fixture.receiverBookId()
        );

        return new ExchangeFixture(
                fixture.sender(),
                fixture.receiver(),
                fixture.senderBookId(),
                fixture.receiverBookId(),
                exchangeId
        );
    }

    private RequestCreateDTO buildRequestCreateDTO(ExchangeFixture fixture) {
        return RequestCreateDTO.builder()
                .receiverUserId(fixture.receiver().getId())
                .senderBookId(fixture.senderBookId())
                .receiverBookId(fixture.receiverBookId())
                .build();
    }

    private String requestPath(Long exchangeId) {
        return ExchangePaths.REQUEST_PATH + "/" + exchangeId;
    }

    private String declinePath(Long exchangeId) {
        return requestPath(exchangeId) + "/decline";
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
