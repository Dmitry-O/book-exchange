package com.example.bookexchange.book.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.book.dto.BookCreateDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.support.FixtureNumbers;
import com.example.bookexchange.support.TestBookCategories;
import com.example.bookexchange.support.TestBookStrings;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.support.fixture.BookFixtureSupport;
import com.example.bookexchange.support.fixture.ExchangeFixtureSupport;
import com.example.bookexchange.support.fixture.UserFixtureSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static com.example.bookexchange.support.PageTestDefaults.PAGE_INDEX;
import static com.example.bookexchange.support.PageTestDefaults.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class BookControllerIT extends IntegrationTestSupport {

    @Autowired
    BookRepository bookRepository;

    @Autowired
    ExchangeRepository exchangeRepository;

    @Autowired
    UserFixtureSupport userUtil;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BookFixtureSupport bookUtil;

    @Autowired
    ExchangeFixtureSupport exchangeUtilIT;

    @Autowired
    TransactionTemplate transactionTemplate;

    MockMvc mockMvc;

    private User user;
    private Long bookId;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc();
    }

    @BeforeAll
    void init() {
        user = userUtil.createUser(null);
        bookId = bookUtil.createBook(user.getId(), null);
    }

    @AfterAll
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            bookUtil.deleteUserBooks(user.getId());
            userUtil.deleteUser(user.getId());
        });

        user = null;
        bookId = null;
    }

    @Test
    void shouldAddUserBook_whenPayloadIsValid() throws Exception {
        BookCreateDTO bookCreateDTO = buildBookCreateDTO(10);

        MvcResult mvcResult = mockMvc.perform(post(BookPaths.BOOK_PATH_USER)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        Long savedId = body.path("data").path("id").asLong();
        Book savedBook = bookRepository.findById(savedId).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("error").isNull()).isTrue();
        assertThat(body.path("message").asText()).isNotBlank();
        assertThat(savedId).isPositive();
        assertThat(savedBook.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedBook.getName()).isEqualTo(bookCreateDTO.getName());
        assertThat(savedBook.getDescription()).isEqualTo(bookCreateDTO.getDescription());
        assertThat(savedBook.getAuthor()).isEqualTo(bookCreateDTO.getAuthor());
        assertThat(savedBook.getCategory()).isEqualTo(bookCreateDTO.getCategory().getProperty());
        assertThat(savedBook.getPublicationYear()).isEqualTo(bookCreateDTO.getPublicationYear());
        assertThat(savedBook.getPhotoUrl()).isEqualTo(expectedBookPhotoUrl(user.getId(), savedId));
        assertThat(body.path("data").path("photoUrl").asText()).isEqualTo(expectedBookPhotoUrl(user.getId(), savedId));
        assertThat(savedBook.getCity()).isEqualTo(bookCreateDTO.getCity());
        assertThat(savedBook.getContactDetails()).isEqualTo(bookCreateDTO.getContactDetails());
        assertThat(savedBook.getIsGift()).isFalse();
        assertThat(savedBook.getIsExchanged()).isFalse();
        assertThat(eTagVersion(mvcResult)).isEqualTo(savedBook.getVersion());
    }

    @Test
    void shouldReturnBadRequest_whenAddUserBookPayloadIsInvalid() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post(BookPaths.BOOK_PATH_USER)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(BookCreateDTO.builder().build())))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertValidationErrorResponse(responseBody(mvcResult), BookPaths.BOOK_PATH_USER);
    }

    @Test
    void shouldReturnUserBooks_whenUserGetsOwnBooks() throws Exception {
        Long secondBookId = bookUtil.createBook(user.getId(), FixtureNumbers.book(2));
        Long exchangedBookId = bookUtil.createBook(user.getId(), FixtureNumbers.book(3));

        Book exchangedBook = bookRepository.findById(exchangedBookId).orElseThrow();
        exchangedBook.setIsExchanged(true);
        bookRepository.save(exchangedBook);

        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_USER)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .queryParam("pageIndex", PAGE_INDEX.toString())
                        .queryParam("pageSize", PAGE_SIZE.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode content = body.path("data").path("content");
        Set<Long> returnedIds = new HashSet<>();

        for (JsonNode bookNode : content) {
            returnedIds.add(bookNode.path("id").asLong());
            assertThat(bookNode.path("isExchanged").asBoolean()).isFalse();
            assertHasVersion(bookNode);
        }

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(2);
        assertThat(content.size()).isEqualTo(2);
        assertThat(returnedIds).contains(bookId, secondBookId);
        assertThat(returnedIds).doesNotContain(exchangedBookId);
    }

    @Test
    void shouldReturnUserBook_whenUserGetsBookById() throws Exception {
        Book persistedBook = bookRepository.findById(bookId).orElseThrow();

        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_USER_BOOK_ID, bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode data = body.path("data");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(data.path("id").asLong()).isEqualTo(bookId);
        assertThat(data.path("name").asText()).isEqualTo(persistedBook.getName());
        assertThat(data.path("description").asText()).isEqualTo(persistedBook.getDescription());
        assertThat(data.path("author").asText()).isEqualTo(persistedBook.getAuthor());
        assertThat(data.path("category").asText()).isEqualTo(persistedBook.getCategory());
        assertThat(data.path("publicationYear").asInt()).isEqualTo(persistedBook.getPublicationYear());
        assertThat(data.path("city").asText()).isEqualTo(persistedBook.getCity());
        assertThat(data.path("contactDetails").asText()).isEqualTo(persistedBook.getContactDetails());
        assertThat(data.path("photoUrl").asText()).isEqualTo(persistedBook.getPhotoUrl());
        assertVersion(data, persistedBook.getVersion());
        assertThat(data.path("isGift").asBoolean()).isEqualTo(persistedBook.getIsGift());
        assertThat(data.path("isExchanged").asBoolean()).isEqualTo(persistedBook.getIsExchanged());
        assertThat(eTagVersion(mvcResult)).isEqualTo(persistedBook.getVersion());
    }

    @Test
    void shouldReturnPublicBook_whenAnonymousUserGetsBookById() throws Exception {
        user.setPhotoUrl(expectedUserPhotoUrl(user.getId()));
        userRepository.saveAndFlush(user);

        Book persistedBook = bookRepository.findById(bookId).orElseThrow();

        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_BOOK_ID, bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode data = body.path("data");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(data.path("id").asLong()).isEqualTo(bookId);
        assertVersion(data, persistedBook.getVersion());
        assertThat(data.path("name").asText()).isEqualTo(persistedBook.getName());
        assertThat(data.path("contactDetails").asText()).isEqualTo(persistedBook.getContactDetails());
        assertThat(data.path("ownerUserId").asLong()).isEqualTo(user.getId());
        assertThat(data.path("ownerNickname").asText()).isEqualTo(user.getNickname());
        assertThat(data.path("ownerPhotoUrl").asText()).isEqualTo(expectedUserPhotoUrl(user.getId()));
        assertThat(data.path("photoUrl").asText()).isEqualTo(persistedBook.getPhotoUrl());
        assertThat(eTagVersion(mvcResult)).isEqualTo(persistedBook.getVersion());
    }

    @Test
    void shouldReturnNotFound_whenAnonymousUserGetsExchangedBookById() throws Exception {
        Long exchangedBookId = bookUtil.createBook(user.getId(), FixtureNumbers.book(25));
        Book exchangedBook = bookRepository.findById(exchangedBookId).orElseThrow();
        exchangedBook.setIsExchanged(true);
        bookRepository.save(exchangedBook);

        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_BOOK_ID, exchangedBookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.BOOK_PUBLIC_NOT_FOUND,
                BookPaths.BOOK_PATH + "/" + exchangedBookId
        );
    }

    @Test
    void shouldReturnNotFound_whenAnonymousUserGetsDeletedBookById() throws Exception {
        Long deletedBookId = bookUtil.createBook(user.getId(), FixtureNumbers.book(26));
        Book deletedBook = bookRepository.findById(deletedBookId).orElseThrow();
        deletedBook.setDeletedAt(java.time.Instant.now());
        bookRepository.save(deletedBook);

        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_BOOK_ID, deletedBookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.BOOK_PUBLIC_NOT_FOUND,
                BookPaths.BOOK_PATH + "/" + deletedBookId
        );
    }

    @Test
    void shouldReturnNotFound_whenUserGetsMissingBookById() throws Exception {
        User anotherUser = userUtil.createUser(FixtureNumbers.book(20));
        Long anotherUserBookId = bookUtil.createBook(anotherUser.getId(), FixtureNumbers.book(20));

        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_USER_BOOK_ID, anotherUserBookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.BOOK_NOT_FOUND,
                userBookPath(anotherUserBookId)
        );
    }

    @Test
    void shouldReturnExchangedBooks_whenUserGetsExchangeHistoryBooks() throws Exception {
        User receiverUser = userUtil.createUser(FixtureNumbers.book(30));
        Long senderBookId = bookUtil.createBook(user.getId(), FixtureNumbers.book(30));
        Long receiverBookId = bookUtil.createBook(receiverUser.getId(), FixtureNumbers.book(31));
        Long exchangeId = exchangeUtilIT.createExchange(user.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).orElseThrow();
        Book senderBook = bookRepository.findById(senderBookId).orElseThrow();

        exchange.setStatus(ExchangeStatus.APPROVED);
        senderBook.setIsExchanged(true);

        exchangeRepository.save(exchange);
        bookRepository.save(senderBook);

        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_HISTORY)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .queryParam("pageIndex", PAGE_INDEX.toString())
                        .queryParam("pageSize", PAGE_SIZE.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode content = body.path("data").path("content");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(content.size()).isEqualTo(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(senderBookId);
        assertHasVersion(content.get(0));
        assertThat(content.get(0).path("isExchanged").asBoolean()).isTrue();
    }

    @Test
    void shouldReturnEmptyPage_whenUserHasNoExchangedBooks() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_HISTORY)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .queryParam("pageIndex", PAGE_INDEX.toString())
                        .queryParam("pageSize", PAGE_SIZE.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isZero();
        assertThat(body.path("data").path("content").size()).isZero();
    }

    @Test
    void shouldReturnBooks_whenPublicSearchMatchesBooks() throws Exception {
        user.setPhotoUrl(expectedUserPhotoUrl(user.getId()));
        userRepository.saveAndFlush(user);

        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_SEARCH)
                        .queryParam("pageIndex", PAGE_INDEX.toString())
                        .queryParam("pageSize", PAGE_SIZE.toString())
                        .queryParam("author", TestBookStrings.author(1))
                        .queryParam("category", TestBookCategories.category(1).getProperty())
                        .queryParam("publicationYear", "2000")
                        .queryParam("city", TestBookStrings.city(1))
                        .queryParam("isGift", "false")
                        .queryParam("searchText", TestBookStrings.name(1))
                        .queryParam("sortBy", "CATEGORY")
                        .queryParam("sortDirection", "ASC"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode content = body.path("data").path("content");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(content.size()).isEqualTo(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(bookId);
        assertThat(content.get(0).path("contactDetails").asText()).isNotBlank();
        assertThat(content.get(0).path("ownerUserId").asLong()).isEqualTo(user.getId());
        assertThat(content.get(0).path("ownerNickname").asText()).isEqualTo(user.getNickname());
        assertThat(content.get(0).path("ownerPhotoUrl").asText()).isEqualTo(expectedUserPhotoUrl(user.getId()));
        assertThat(content.get(0).path("photoUrl").asText()).isEqualTo(expectedBookPhotoUrl(user.getId(), bookId));
        assertHasVersion(content.get(0));
    }

    @Test
    void shouldReturnBadRequest_whenPublicSearchPayloadIsInvalid() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_SEARCH)
                        .queryParam("pageIndex", PAGE_INDEX.toString())
                        .queryParam("pageSize", PAGE_SIZE.toString())
                        .queryParam("searchText", "ab"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertValidationErrorResponse(responseBody(mvcResult), BookPaths.BOOK_PATH_SEARCH);
    }

    @Test
    void shouldExcludeOwnBooks_whenAuthenticatedUserSearchesPublicBooks() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_SEARCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .queryParam("pageIndex", PAGE_INDEX.toString())
                        .queryParam("pageSize", PAGE_SIZE.toString())
                        .queryParam("searchText", TestBookStrings.name(1)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isZero();
        assertThat(body.path("data").path("content").size()).isZero();
    }

    @Test
    void shouldDeleteBook_whenUserDeletesBookById() throws Exception {
        Long bookToDeleteId = bookUtil.createBook(user.getId(), FixtureNumbers.book(40));

        MvcResult mvcResult = mockMvc.perform(delete(BookPaths.BOOK_PATH_USER_BOOK_ID, bookToDeleteId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .header(HttpHeaders.IF_MATCH, bookIfMatch(bookToDeleteId)))
                .andExpect(status().isOk())
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        Book deletedBook = bookRepository.findById(bookToDeleteId).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").isNull()).isTrue();
        assertThat(body.path("message").asText()).isNotBlank();
        assertThat(deletedBook.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldReturnNotFound_whenUserDeletesMissingBookById() throws Exception {
        User anotherUser = userUtil.createUser(FixtureNumbers.book(50));
        Long anotherUserBookId = bookUtil.createBook(anotherUser.getId(), FixtureNumbers.book(50));

        MvcResult mvcResult = mockMvc.perform(delete(BookPaths.BOOK_PATH_USER_BOOK_ID, anotherUserBookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .header(HttpHeaders.IF_MATCH, bookIfMatch(anotherUserBookId)))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.BOOK_NOT_FOUND,
                userBookPath(anotherUserBookId)
        );
    }

    @Test
    void shouldReturnConflict_whenUserDeletesBookWithStaleVersion() throws Exception {
        Long bookToDeleteId = bookUtil.createBook(user.getId(), FixtureNumbers.book(60));

        MvcResult mvcResult = mockMvc.perform(delete(BookPaths.BOOK_PATH_USER_BOOK_ID, bookToDeleteId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .header(HttpHeaders.IF_MATCH, "\"999\""))
                .andExpect(status().isConflict())
                .andReturn();

        clearPersistenceContext();

        assertErrorResponse(
                responseBody(mvcResult),
                409,
                MessageKey.SYSTEM_OPTIMISTIC_LOCK,
                userBookPath(bookToDeleteId)
        );
        assertThat(bookRepository.findById(bookToDeleteId)).isPresent();
    }

    @Test
    void shouldUpdateBook_whenUserUpdatesBookById() throws Exception {
        Long bookToUpdateId = bookUtil.createBook(user.getId(), FixtureNumbers.book(70));
        BookUpdateDTO bookUpdateDTO = buildBookUpdateDTO(FixtureNumbers.book(70));

        MvcResult mvcResult = mockMvc.perform(patch(BookPaths.BOOK_PATH_USER_BOOK_ID, bookToUpdateId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .header(HttpHeaders.IF_MATCH, bookIfMatch(bookToUpdateId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookUpdateDTO)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        Book updatedBook = bookRepository.findById(bookToUpdateId).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(bookToUpdateId);
        assertThat(body.path("data").path("name").asText()).isEqualTo(bookUpdateDTO.getName());
        assertThat(body.path("data").path("city").asText()).isEqualTo(bookUpdateDTO.getCity());
        assertThat(body.path("data").path("isGift").asBoolean()).isEqualTo(bookUpdateDTO.getIsGift());
        assertThat(updatedBook.getName()).isEqualTo(bookUpdateDTO.getName());
        assertThat(updatedBook.getDescription()).isEqualTo(bookUpdateDTO.getDescription());
        assertThat(updatedBook.getAuthor()).isEqualTo(bookUpdateDTO.getAuthor());
        assertThat(updatedBook.getCategory()).isEqualTo(bookUpdateDTO.getCategory().getProperty());
        assertThat(updatedBook.getPublicationYear()).isEqualTo(bookUpdateDTO.getPublicationYear());
        assertThat(updatedBook.getPhotoUrl()).isEqualTo(expectedBookPhotoUrl(user.getId(), bookToUpdateId));
        assertThat(body.path("data").path("photoUrl").asText()).isEqualTo(expectedBookPhotoUrl(user.getId(), bookToUpdateId));
        assertThat(updatedBook.getCity()).isEqualTo(bookUpdateDTO.getCity());
        assertThat(updatedBook.getContactDetails()).isEqualTo(bookUpdateDTO.getContactDetails());
        assertThat(updatedBook.getIsGift()).isEqualTo(bookUpdateDTO.getIsGift());
        assertThat(mvcResult.getResponse().getHeader(HttpHeaders.ETAG)).isNotBlank();
    }

    @Test
    void shouldDeleteBookPhoto_whenUserDeletesBookPhoto() throws Exception {
        Long bookWithPhotoId = bookUtil.createBook(user.getId(), FixtureNumbers.book(701));

        MvcResult mvcResult = mockMvc.perform(delete(BookPaths.BOOK_PATH_USER_BOOK_ID_PHOTO, bookWithPhotoId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .header(HttpHeaders.IF_MATCH, bookIfMatch(bookWithPhotoId))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        Book updatedBook = bookRepository.findById(bookWithPhotoId).orElseThrow();
        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("photoUrl").isNull()).isTrue();
        assertThat(updatedBook.getPhotoUrl()).isNull();
    }

    @Test
    void shouldReturnNotFound_whenUserUpdatesMissingBookById() throws Exception {
        User anotherUser = userUtil.createUser(FixtureNumbers.book(80));
        Long anotherUserBookId = bookUtil.createBook(anotherUser.getId(), FixtureNumbers.book(80));

        MvcResult mvcResult = mockMvc.perform(patch(BookPaths.BOOK_PATH_USER_BOOK_ID, anotherUserBookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .header(HttpHeaders.IF_MATCH, bookIfMatch(anotherUserBookId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBookUpdateDTO(80))))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.BOOK_NOT_FOUND,
                userBookPath(anotherUserBookId)
        );
    }

    @Test
    void shouldReturnBadRequest_whenUserUpdateBookPayloadIsInvalid() throws Exception {
        Long bookToUpdateId = bookUtil.createBook(user.getId(), FixtureNumbers.book(90));

        BookUpdateDTO invalidBookUpdateDTO = BookUpdateDTO.builder()
                .name("ab")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(BookPaths.BOOK_PATH_USER_BOOK_ID, bookToUpdateId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .header(HttpHeaders.IF_MATCH, bookIfMatch(bookToUpdateId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBookUpdateDTO)))
                .andExpect(status().isBadRequest())
                .andReturn();

        clearPersistenceContext();

        assertValidationErrorResponse(responseBody(mvcResult), userBookPath(bookToUpdateId));
        assertThat(bookRepository.findById(bookToUpdateId).orElseThrow().getName())
                .isEqualTo(TestBookStrings.name(FixtureNumbers.book(90)));
    }

    @Test
    void shouldReturnConflict_whenUserUpdatesBookWithStaleVersion() throws Exception {
        Long bookToUpdateId = bookUtil.createBook(user.getId(), FixtureNumbers.book(100));
        BookUpdateDTO bookUpdateDTO = buildBookUpdateDTO(FixtureNumbers.book(100));

        MvcResult mvcResult = mockMvc.perform(patch(BookPaths.BOOK_PATH_USER_BOOK_ID, bookToUpdateId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .header(HttpHeaders.IF_MATCH, "\"999\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookUpdateDTO)))
                .andExpect(status().isConflict())
                .andReturn();

        clearPersistenceContext();

        assertErrorResponse(
                responseBody(mvcResult),
                409,
                MessageKey.SYSTEM_OPTIMISTIC_LOCK,
                userBookPath(bookToUpdateId)
        );
        assertThat(bookRepository.findById(bookToUpdateId).orElseThrow().getName())
                .isEqualTo(TestBookStrings.name(FixtureNumbers.book(100)));
    }

    private BookCreateDTO buildBookCreateDTO(int bookNumber) {
        return BookCreateDTO.builder()
                .name(TestBookStrings.name(bookNumber))
                .description(TestBookStrings.description(bookNumber))
                .author(TestBookStrings.author(bookNumber))
                .category(TestBookCategories.category(bookNumber))
                .publicationYear(2000)
                .photoBase64(validPhotoBase64("photo-" + bookNumber))
                .city(TestBookStrings.city(bookNumber))
                .contactDetails(TestBookStrings.contactDetails(bookNumber))
                .isGift(false)
                .build();
    }

    private BookUpdateDTO buildBookUpdateDTO(int bookNumber) {
        return BookUpdateDTO.builder()
                .name(TestBookStrings.updatedName(bookNumber))
                .description(TestBookStrings.updatedDescription(bookNumber))
                .author(TestBookStrings.updatedAuthor(bookNumber))
                .category(TestBookCategories.updatedCategory(bookNumber))
                .publicationYear(2010)
                .photoBase64(validPhotoBase64("updated-photo-" + bookNumber))
                .city(TestBookStrings.updatedCity(bookNumber))
                .contactDetails(TestBookStrings.updatedContactDetails(bookNumber))
                .isGift(true)
                .build();
    }

    private String bookIfMatch(Long currentBookId) {
        clearPersistenceContext();
        Long version = bookRepository.findById(currentBookId).orElseThrow().getVersion();
        return "\"" + version + "\"";
    }

    private String userBookPath(Long currentBookId) {
        return BookPaths.BOOK_PATH_USER + "/" + currentBookId;
    }

    private String validPhotoBase64(String value) {
        return Base64.getEncoder()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
