package com.example.bookexchange.admin.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.support.fixture.BookFixtureSupport;
import com.example.bookexchange.support.PageTestDefaults;
import com.example.bookexchange.support.FixtureNumbers;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class AdminBookControllerIT extends IntegrationTestSupport {

    @Autowired
    BookRepository bookRepository;

    @Autowired
    UserFixtureSupport userUtil;

    @Autowired
    BookFixtureSupport bookUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldReturnActiveBooks_whenAdminSearchesBooks() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminBook(1));
        User owner = userUtil.createUser(FixtureNumbers.adminBook(2));
        Long deletedBookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(3));
        Long activeBookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(4));

        Book deletedBook = bookRepository.findById(deletedBookId).orElseThrow();
        deletedBook.setDeletedAt(Instant.parse("2026-04-05T10:15:30Z"));
        bookRepository.save(deletedBook);

        clearPersistenceContext();

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_BOOKS_SEARCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("bookType", BookType.ACTIVE.name())
                        .queryParam("searchText", "Book " + FixtureNumbers.adminBook(4))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode content = body.path("data").path("content");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(activeBookId);
        assertThat(content.get(0).path("ownerUserId").asLong()).isEqualTo(owner.getId());
        assertThat(content.get(0).path("ownerNickname").asText()).isEqualTo(owner.getNickname());
        assertThat(content.get(0).path("meta").path("deletedAt").isNull()).isTrue();
        assertThat(content.get(0).path("id").asLong()).isNotEqualTo(deletedBookId);
    }

    @Test
    void shouldReturnDeletedBooks_whenAdminSearchesDeletedBooks() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminBook(21));
        User owner = userUtil.createUser(FixtureNumbers.adminBook(22));
        Long deletedBookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(23));
        Long activeBookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(24));

        Book deletedBook = bookRepository.findById(deletedBookId).orElseThrow();
        deletedBook.setDeletedAt(Instant.parse("2026-04-05T10:35:00Z"));
        bookRepository.saveAndFlush(deletedBook);

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_BOOKS_SEARCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("bookType", BookType.DELETED.name())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = responseBody(mvcResult).path("data").path("content");

        assertThat(content.toString()).contains("\"id\":" + deletedBookId);
        assertThat(content.toString()).doesNotContain("\"id\":" + activeBookId);
    }

    @Test
    void shouldReturnActiveAndDeletedBooks_whenAdminSearchesAllBooks() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminBook(25));
        User owner = userUtil.createUser(FixtureNumbers.adminBook(26));
        Long deletedBookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(27));
        Long activeBookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(28));

        Book deletedBook = bookRepository.findById(deletedBookId).orElseThrow();
        deletedBook.setDeletedAt(Instant.parse("2026-04-05T10:40:00Z"));
        bookRepository.saveAndFlush(deletedBook);

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_BOOKS_SEARCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("bookType", BookType.ALL.name())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = responseBody(mvcResult).path("data").path("content");

        assertThat(content.toString()).contains("\"id\":" + activeBookId);
        assertThat(content.toString()).contains("\"id\":" + deletedBookId);
    }

    @Test
    void shouldReturnDeletedBook_whenAdminGetsDeletedBookById() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminBook(5));
        User owner = userUtil.createUser(FixtureNumbers.adminBook(6));
        Long deletedBookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(7));

        Book deletedBook = bookRepository.findById(deletedBookId).orElseThrow();
        deletedBook.setDeletedAt(Instant.parse("2026-04-05T10:20:00Z"));
        bookRepository.save(deletedBook);

        clearPersistenceContext();

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_BOOKS_ID, deletedBookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(deletedBookId);
        assertThat(body.path("data").path("ownerUserId").asLong()).isEqualTo(owner.getId());
        assertThat(body.path("data").path("ownerNickname").asText()).isEqualTo(owner.getNickname());
        assertThat(body.path("data").path("meta").path("deletedAt").asText()).isNotBlank();
    }

    @Test
    void shouldSortDeletedBooksByNameDescending_whenAdminRequestsNameDescSort() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminBook(29));
        User owner = userUtil.createUser(FixtureNumbers.adminBook(30));
        Long alphaBookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(31));
        Long zuluBookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(32));

        Book alphaBook = bookRepository.findById(alphaBookId).orElseThrow();
        alphaBook.setName("Alpha deleted sort");
        alphaBook.setDeletedAt(Instant.parse("2026-04-05T10:45:00Z"));

        Book zuluBook = bookRepository.findById(zuluBookId).orElseThrow();
        zuluBook.setName("Zulu deleted sort");
        zuluBook.setDeletedAt(Instant.parse("2026-04-05T10:46:00Z"));

        bookRepository.save(alphaBook);
        bookRepository.saveAndFlush(zuluBook);

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_BOOKS_SEARCH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("bookType", BookType.DELETED.name())
                        .queryParam("searchText", "deleted sort")
                        .queryParam("sortBy", "NAME")
                        .queryParam("sortDirection", "DESC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = responseBody(mvcResult).path("data").path("content");

        assertThat(content.get(0).path("id").asLong()).isEqualTo(zuluBookId);
        assertThat(content.get(1).path("id").asLong()).isEqualTo(alphaBookId);
    }

    @Test
    void shouldReturnNotFound_whenAdminGetsMissingBookById() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminBook(20));

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_BOOKS_ID, Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.ADMIN_BOOK_NOT_FOUND,
                AdminPaths.ADMIN_PATH_BOOKS + "/" + Long.MAX_VALUE
        );
    }

    @Test
    void shouldUpdateBook_whenAdminUpdatesBookById() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminBook(8));
        User owner = userUtil.createUser(FixtureNumbers.adminBook(9));
        Long bookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(10));

        BookUpdateDTO dto = BookUpdateDTO.builder()
                .name("Updated admin book")
                .description("Updated book description for admin flow")
                .author("Updated admin author")
                .category("Updated category")
                .publicationYear(2021)
                .photoBase64(validBase64("updated-admin-book"))
                .city("Berlin")
                .contactDetails("updated-admin@example.com")
                .isGift(true)
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_BOOKS_ID, bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, ifMatch(bookRepository.findById(bookId).orElseThrow().getVersion()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        Book updatedBook = bookRepository.findById(bookId).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(bookId);
        assertThat(body.path("data").path("name").asText()).isEqualTo(dto.getName());
        assertThat(updatedBook.getName()).isEqualTo(dto.getName());
        assertThat(updatedBook.getCity()).isEqualTo(dto.getCity());
        assertThat(updatedBook.getIsGift()).isEqualTo(dto.getIsGift());
    }

    @Test
    void shouldSoftDeleteBook_whenAdminDeletesBookById() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminBook(11));
        User owner = userUtil.createUser(FixtureNumbers.adminBook(12));
        Long bookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(13));
        Long version = bookRepository.findById(bookId).orElseThrow().getVersion();

        MvcResult mvcResult = mockMvc.perform(delete(AdminPaths.ADMIN_PATH_BOOKS_ID, bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, ifMatch(version))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        Book deletedBook = bookRepository.findById(bookId).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(bookId);
        assertThat(deletedBook.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldRestoreBook_whenAdminRestoresBookById() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminBook(14));
        User owner = userUtil.createUser(FixtureNumbers.adminBook(15));
        Long bookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(16));
        Book deletedBook = bookRepository.findById(bookId).orElseThrow();
        deletedBook.setDeletedAt(Instant.parse("2026-04-05T10:30:00Z"));
        bookRepository.save(deletedBook);

        clearPersistenceContext();

        Long version = bookRepository.findById(bookId).orElseThrow().getVersion();

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_BOOKS_ID_RESTORE, bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, ifMatch(version))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        Book restoredBook = bookRepository.findById(bookId).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(bookId);
        assertThat(restoredBook.getDeletedAt()).isNull();
    }

    @Test
    void shouldReturnConflict_whenAdminUpdatesBookWithStaleVersion() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminBook(17));
        User owner = userUtil.createUser(FixtureNumbers.adminBook(18));
        Long bookId = bookUtil.createBook(owner.getId(), FixtureNumbers.adminBook(19));
        BookUpdateDTO dto = BookUpdateDTO.builder()
                .name("Conflict case title")
                .description("Conflict case description.")
                .author("Conflict author")
                .category("Conflict category")
                .publicationYear(2022)
                .photoBase64(validBase64("conflict-admin-book"))
                .city("Munich")
                .contactDetails("conflict-admin@example.com")
                .isGift(false)
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_BOOKS_ID, bookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, "\"999\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                409,
                MessageKey.SYSTEM_OPTIMISTIC_LOCK,
                AdminPaths.ADMIN_PATH_BOOKS + "/" + bookId
        );
    }
}
