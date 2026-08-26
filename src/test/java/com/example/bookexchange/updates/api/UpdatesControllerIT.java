package com.example.bookexchange.updates.api;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.notification.UserUpdate;
import com.example.bookexchange.common.notification.UserUpdateRepository;
import com.example.bookexchange.common.notification.UserUpdateType;
import com.example.bookexchange.exchange.api.ExchangePaths;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.support.FixtureNumbers;
import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.support.PageTestDefaults;
import com.example.bookexchange.support.TestBookStrings;
import com.example.bookexchange.support.fixture.BookFixtureSupport;
import com.example.bookexchange.support.fixture.ExchangeFixtureSupport;
import com.example.bookexchange.support.fixture.UserFixtureSupport;
import com.example.bookexchange.user.model.User;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class UpdatesControllerIT extends IntegrationTestSupport {

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

    @Autowired
    UserUpdateRepository userUpdateRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldExposeUpdatesUnderDedicatedSwaggerTag() throws Exception {
        JsonNode openApi = responseBody(mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(openApi.path("paths").path(UpdatesPaths.UPDATES_PATH).path("get").path("tags").get(0).asText())
                .isEqualTo("Updates");
        assertThat(openApi.path("paths").path("/history").path("get").path("tags").get(0).asText())
                .isEqualTo("Exchange history");
    }

    @Test
    void shouldReturnUnreadUpdates_whenReceiverGetsNewExchangeRequest() throws Exception {
        ExchangeFixture fixture = createPendingExchange(620);

        MvcResult mvcResult = mockMvc.perform(get(UpdatesPaths.UPDATES_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("readState", "UNREAD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode content = body.path("data").path("content");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(content.get(0).path("exchangeId").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(content.get(0).path("userExchangeRole").asText()).isEqualTo("RECEIVER");
        assertThat(content.get(0).path("isRead").asBoolean()).isFalse();
        assertThat(content.get(0).path("otherBookName").asText()).isNotBlank();
        assertThat(content.get(0).path("otherUserId").asLong()).isEqualTo(fixture.sender().getId());
        assertThat(content.get(0).path("otherUserNickname").asText()).isEqualTo(fixture.sender().getNickname());
        assertThat(content.get(0).path("senderBook").path("id").asLong()).isPositive();
        assertThat(content.get(0).path("senderBook").path("name").asText()).isNotBlank();
        assertThat(content.get(0).path("receiverBook").path("id").asLong()).isPositive();
        assertThat(content.get(0).path("receiverBook").path("name").asText()).isNotBlank();
        assertThat(content.get(0).path("updateCreatedAt").asText()).isNotBlank();
        assertHasVersion(content.get(0));
    }

    @Test
    void shouldReturnUnreadUpdatesFromDedicatedUnreadEndpoint() throws Exception {
        ExchangeFixture fixture = createPendingExchange(619);

        MvcResult mvcResult = mockMvc.perform(get(UpdatesPaths.UPDATES_PATH_UNREAD)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = responseBody(mvcResult).path("data").path("content");

        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("exchangeId").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(content.get(0).path("isRead").asBoolean()).isFalse();
    }

    @Test
    void shouldReturnGiftUnreadUpdatesWithNullOtherBook_whenReceiverGetsGiftRequest() throws Exception {
        User sender = userUtil.createUser(FixtureNumbers.history(621));
        User receiver = userUtil.createUser(FixtureNumbers.history(622));
        Long receiverBookId = bookUtil.createBook(receiver.getId(), FixtureNumbers.history(622));
        Exchange exchange = createGiftExchange(sender, receiver, receiverBookId);

        MvcResult mvcResult = mockMvc.perform(get(UpdatesPaths.UPDATES_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(receiver))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("readState", "UNREAD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = responseBody(mvcResult).path("data").path("content");

        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(exchange.getId());
        assertThat(content.get(0).path("exchangeId").asLong()).isEqualTo(exchange.getId());
        assertThat(content.get(0).path("userExchangeRole").asText()).isEqualTo("RECEIVER");
        assertThat(content.get(0).path("isRead").asBoolean()).isFalse();
        assertThat(content.get(0).get("otherBookId").isNull()).isTrue();
        assertThat(content.get(0).get("otherBookName").isNull()).isTrue();
        assertThat(content.get(0).get("senderBook").isNull()).isTrue();
        assertThat(content.get(0).path("receiverBook").path("id").asLong()).isEqualTo(receiverBookId);
        assertThat(content.get(0).path("receiverBook").path("isGift").asBoolean()).isTrue();
        assertThat(content.get(0).path("otherUserId").asLong()).isEqualTo(sender.getId());
        assertThat(content.get(0).path("otherUserNickname").asText()).isEqualTo(sender.getNickname());
    }

    @Test
    void shouldKeepOwnCreatedRequestUnreadUntilSenderOpensIt() throws Exception {
        ExchangeFixture fixture = createPendingExchange(623);

        MvcResult unreadBeforeOpenResult = mockMvc.perform(get(UpdatesPaths.UPDATES_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("readState", "UNREAD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode unreadBeforeOpenContent = responseBody(unreadBeforeOpenResult).path("data").path("content");

        assertThat(unreadBeforeOpenContent).hasSize(1);
        assertThat(unreadBeforeOpenContent.get(0).path("exchangeId").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(unreadBeforeOpenContent.get(0).path("userExchangeRole").asText()).isEqualTo("SENDER");
        assertThat(unreadBeforeOpenContent.get(0).path("isRead").asBoolean()).isFalse();

        mockMvc.perform(get(ExchangePaths.REQUEST_PATH_EXCHANGE_ID, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        clearPersistenceContext();

        Exchange openedExchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        assertThat(openedExchange.getIsReadBySender()).isTrue();
        assertThat(openedExchange.getIsReadByReceiver()).isFalse();

        MvcResult unreadAfterOpenResult = mockMvc.perform(get(UpdatesPaths.UPDATES_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("readState", "UNREAD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(responseBody(unreadAfterOpenResult).path("data").path("totalElements").asLong()).isZero();
    }

    @Test
    void shouldReturnAllExchangeUpdatesInChronologicalOrderAndKeepReadState() throws Exception {
        User receiver = userUtil.createUser(FixtureNumbers.history(628));
        User firstSender = userUtil.createUser(FixtureNumbers.history(629));
        User secondSender = userUtil.createUser(FixtureNumbers.history(630));
        Long receiverBookId = bookUtil.createBook(receiver.getId(), FixtureNumbers.history(628));
        Long firstSenderBookId = bookUtil.createBook(firstSender.getId(), FixtureNumbers.history(629));
        Long secondSenderBookId = bookUtil.createBook(secondSender.getId(), FixtureNumbers.history(630));
        Long firstExchangeId = exchangeUtilIT.createExchange(firstSender.getId(), receiver.getId(), firstSenderBookId, receiverBookId);
        Long secondExchangeId = exchangeUtilIT.createExchange(secondSender.getId(), receiver.getId(), secondSenderBookId, receiverBookId);

        mockMvc.perform(get(ExchangePaths.OFFER_PATH_EXCHANGE_ID, secondExchangeId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(receiver))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        MvcResult allUpdatesResult = mockMvc.perform(get(UpdatesPaths.UPDATES_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(receiver))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("readState", "ALL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode allContent = responseBody(allUpdatesResult).path("data").path("content");

        assertThat(allContent).hasSize(2);
        assertThat(allContent.get(0).path("exchangeId").asLong()).isEqualTo(secondExchangeId);
        assertThat(allContent.get(0).path("isRead").asBoolean()).isTrue();
        assertThat(allContent.get(1).path("exchangeId").asLong()).isEqualTo(firstExchangeId);
        assertThat(allContent.get(1).path("isRead").asBoolean()).isFalse();
        assertThat(allContent.get(0).path("updateCreatedAt").asText())
                .isGreaterThanOrEqualTo(allContent.get(1).path("updateCreatedAt").asText());

        MvcResult readUpdatesResult = mockMvc.perform(get(UpdatesPaths.UPDATES_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(receiver))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("readState", "READ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode readContent = responseBody(readUpdatesResult).path("data").path("content");

        assertThat(readContent).hasSize(1);
        assertThat(readContent.get(0).path("exchangeId").asLong()).isEqualTo(secondExchangeId);
        assertThat(readContent.get(0).path("isRead").asBoolean()).isTrue();

        MvcResult unreadUpdatesResult = mockMvc.perform(get(UpdatesPaths.UPDATES_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(receiver))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("readState", "UNREAD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode unreadContent = responseBody(unreadUpdatesResult).path("data").path("content");

        assertThat(unreadContent).hasSize(1);
        assertThat(unreadContent.get(0).path("exchangeId").asLong()).isEqualTo(firstExchangeId);
        assertThat(unreadContent.get(0).path("isRead").asBoolean()).isFalse();
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

        MvcResult noUnreadResult = mockMvc.perform(get(UpdatesPaths.UPDATES_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("readState", "UNREAD")
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

        MvcResult unreadAgainResult = mockMvc.perform(get(UpdatesPaths.UPDATES_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("readState", "UNREAD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode unreadContent = responseBody(unreadAgainResult).path("data").path("content");

        assertThat(unreadContent).hasSize(1);
        assertThat(unreadContent.get(0).path("id").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(unreadContent.get(0).path("exchangeId").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(unreadContent.get(0).path("status").asText()).isEqualTo(ExchangeStatus.DECLINED.name());
        assertThat(unreadContent.get(0).path("userExchangeRole").asText()).isEqualTo("RECEIVER");
        assertThat(unreadContent.get(0).path("isRead").asBoolean()).isFalse();
        assertThat(unreadContent.get(0).path("declinerUserId").asLong()).isEqualTo(fixture.sender().getId());
        assertThat(unreadContent.get(0).path("declinerUserNickname").asText()).isEqualTo(fixture.sender().getNickname());
        assertThat(unreadContent.get(0).path("declinerUserRole").asText()).isEqualTo("SENDER");
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

        MvcResult mvcResult = mockMvc.perform(get(UpdatesPaths.UPDATES_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.sender()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("readState", "UNREAD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = responseBody(mvcResult).path("data").path("content");

        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(content.get(0).path("exchangeId").asLong()).isEqualTo(fixture.exchangeId());
        assertThat(content.get(0).path("status").asText()).isEqualTo(ExchangeStatus.DECLINED.name());
        assertThat(content.get(0).path("userExchangeRole").asText()).isEqualTo("SENDER");
        assertThat(content.get(0).path("isRead").asBoolean()).isFalse();
        assertThat(content.get(0).path("otherUserId").asLong()).isEqualTo(fixture.receiver().getId());
        assertThat(content.get(0).path("otherUserNickname").asText()).isEqualTo(fixture.receiver().getNickname());
        assertThat(content.get(0).path("declinerUserId").asLong()).isEqualTo(fixture.receiver().getId());
        assertThat(content.get(0).path("declinerUserNickname").asText()).isEqualTo(fixture.receiver().getNickname());
        assertThat(content.get(0).path("declinerUserRole").asText()).isEqualTo("RECEIVER");
    }

    @Test
    void shouldToggleUpdateReadStateWithoutChangingUpdateCreatedAt() throws Exception {
        ExchangeFixture fixture = createPendingExchange(646);

        MvcResult beforeToggleResult = mockMvc.perform(get(UpdatesPaths.UPDATES_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("readState", "UNREAD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode beforeToggle = responseBody(beforeToggleResult).path("data").path("content").get(0);

        clearPersistenceContext();

        Exchange exchangeBeforeToggle = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        String updateCreatedAt = exchangeBeforeToggle.getUpdateCreatedAt().toString();

        assertThat(beforeToggle.path("updateCreatedAt").asText()).isNotBlank();

        MvcResult markReadResult = mockMvc.perform(patch(UpdatesPaths.UPDATES_PATH_EXCHANGE_ID_READ_STATE, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("isRead", true)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        clearPersistenceContext();

        Exchange markedReadExchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        JsonNode markReadBody = responseBody(markReadResult).path("data");

        assertThat(markedReadExchange.getIsReadByReceiver()).isTrue();
        assertThat(markReadBody.path("isRead").asBoolean()).isTrue();
        assertThat(markReadBody.path("updateCreatedAt").asText()).isEqualTo(updateCreatedAt);
        assertThat(markedReadExchange.getUpdateCreatedAt().toString()).isEqualTo(updateCreatedAt);

        MvcResult markUnreadResult = mockMvc.perform(patch(UpdatesPaths.UPDATES_PATH_EXCHANGE_ID_READ_STATE, fixture.exchangeId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("isRead", false)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        clearPersistenceContext();

        Exchange markedUnreadExchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        JsonNode markUnreadBody = responseBody(markUnreadResult).path("data");

        assertThat(markedUnreadExchange.getIsReadByReceiver()).isFalse();
        assertThat(markUnreadBody.path("isRead").asBoolean()).isFalse();
        assertThat(markUnreadBody.path("updateCreatedAt").asText()).isEqualTo(updateCreatedAt);
        assertThat(markedUnreadExchange.getUpdateCreatedAt().toString()).isEqualTo(updateCreatedAt);
    }

    @Test
    void shouldUpdateReadStateForNotificationUpdate() throws Exception {
        User user = userUtil.createUser(FixtureNumbers.history(647));
        UserUpdate notification = createUnreadNotification(user);

        MvcResult mvcResult = mockMvc.perform(patch(
                        UpdatesPaths.UPDATES_PATH_NOTIFICATION_ID_READ_STATE,
                        notification.getId()
                )
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("isRead", true)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        clearPersistenceContext();

        UserUpdate updatedNotification = userUpdateRepository.findById(notification.getId()).orElseThrow();
        JsonNode body = responseBody(mvcResult).path("data");

        assertThat(updatedNotification.getIsRead()).isTrue();
        assertThat(body.path("notificationId").asLong()).isEqualTo(notification.getId());
        assertThat(body.path("isRead").asBoolean()).isTrue();
    }

    @Test
    void shouldMarkAllUpdatesAsRead() throws Exception {
        ExchangeFixture fixture = createPendingExchange(648);
        UserUpdate notification = createUnreadNotification(fixture.receiver());

        mockMvc.perform(patch(UpdatesPaths.UPDATES_PATH_MARK_ALL_READ)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.receiver()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        clearPersistenceContext();

        Exchange updatedExchange = exchangeRepository.findById(fixture.exchangeId()).orElseThrow();
        UserUpdate updatedNotification = userUpdateRepository.findById(notification.getId()).orElseThrow();

        assertThat(updatedExchange.getIsReadByReceiver()).isTrue();
        assertThat(updatedNotification.getIsRead()).isTrue();
    }


    private ExchangeFixture createPendingExchange(int base) {
        int senderNumber = FixtureNumbers.history(base);
        int receiverNumber = FixtureNumbers.history(base + 1);
        User sender = userUtil.createUser(senderNumber);
        User receiver = userUtil.createUser(receiverNumber);
        Long senderBookId = bookUtil.createBook(sender.getId(), senderNumber);
        Long receiverBookId = bookUtil.createBook(receiver.getId(), receiverNumber);
        Long exchangeId = exchangeUtilIT.createExchange(sender.getId(), receiver.getId(), senderBookId, receiverBookId);
        Exchange exchange = exchangeRepository.findById(exchangeId).orElseThrow();

        exchange.setStatus(ExchangeStatus.PENDING);
        exchangeRepository.save(exchange);

        return new ExchangeFixture(
                sender,
                receiver,
                exchangeId,
                TestBookStrings.contactDetails(senderNumber),
                TestBookStrings.contactDetails(receiverNumber)
        );
    }

    private Exchange createGiftExchange(User sender, User receiver, Long receiverBookId) {
        Book receiverBook = bookRepository.findById(receiverBookId).orElseThrow();
        receiverBook.setIsGift(true);
        bookRepository.save(receiverBook);

        Long exchangeId = exchangeUtilIT.createExchange(sender.getId(), receiver.getId(), null, receiverBookId);

        return exchangeRepository.findById(exchangeId).orElseThrow();
    }

    private UserUpdate createUnreadNotification(User user) {
        UserUpdate notification = new UserUpdate();
        notification.setUser(user);
        notification.setType(UserUpdateType.REPORT_RESOLVED);
        notification.setIsRead(false);
        notification.setTargetUrl("/app/my-reports");

        return userUpdateRepository.saveAndFlush(notification);
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
