package com.example.bookexchange.common.api;

import com.example.bookexchange.book.dto.BookCategoryDTO;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.support.FixtureNumbers;
import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.support.fixture.UserFixtureSupport;
import com.example.bookexchange.user.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class MetadataControllerIT extends IntegrationTestSupport {

    private static final String REMOVED_CATEGORY_VALUE = "Hor" + "ror";

    private MockMvc mockMvc;

    @Autowired
    private UserFixtureSupport userUtil;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldReturnBookCategories_whenMetadataIsRequested() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(MetadataPaths.METADATA_PATH)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode categories = body.path("data").path("bookCategories");
        JsonNode links = body.path("data").path("links");
        JsonNode statistics = body.path("data").path("statistics");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(categories.isArray()).isTrue();
        assertThat(categories.toString()).contains("Drama");
        assertThat(categories.toString()).contains("Fantasy");
        assertThat(categories.toString()).contains("Science Fiction");
        assertThat(categories.toString()).contains("Other");
        assertThat(categories.toString()).doesNotContain(REMOVED_CATEGORY_VALUE);
        assertThat(links.path("backendGithubUrl").asText()).isEqualTo("https://github.com/test/book-exchange-backend");
        assertThat(links.path("frontendGithubUrl").asText()).isEqualTo("https://github.com/test/book-exchange-frontend");
        assertThat(links.path("linkedinUrl").asText()).isEqualTo("https://www.linkedin.com/in/test-user");
        assertThat(links.path("swaggerUrl").asText()).isEqualTo("http://localhost:8080/swagger-ui/index.html");
        assertThat(statistics.path("users").isNumber()).isTrue();
        assertThat(statistics.path("books").isNumber()).isTrue();
        assertThat(statistics.path("exchanges").isNumber()).isTrue();
    }

    @Test
    void shouldReturnPopularBookCategoriesOrderedByAvailableBookCount() throws Exception {
        User owner = userUtil.createUser(FixtureNumbers.user(850));

        saveBook(owner, FixtureNumbers.book(850), BookCategoryDTO.COOKING.getProperty(), false, false);
        saveBook(owner, FixtureNumbers.book(851), BookCategoryDTO.COOKING.getProperty(), false, false);
        saveBook(owner, FixtureNumbers.book(852), BookCategoryDTO.COOKING.getProperty(), false, false);
        saveBook(owner, FixtureNumbers.book(853), BookCategoryDTO.POETRY.getProperty(), false, false);
        saveBook(owner, FixtureNumbers.book(854), BookCategoryDTO.POETRY.getProperty(), false, false);
        saveBook(owner, FixtureNumbers.book(855), BookCategoryDTO.TRAVEL.getProperty(), true, false);
        saveBook(owner, FixtureNumbers.book(856), BookCategoryDTO.BIOGRAPHY.getProperty(), false, true);
        saveBook(owner, FixtureNumbers.book(857), REMOVED_CATEGORY_VALUE, false, false);

        MvcResult mvcResult = mockMvc.perform(get(MetadataPaths.METADATA_PATH)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode popularCategories = responseBody(mvcResult).path("data").path("popularBookCategories");

        assertThat(popularCategories.isArray()).isTrue();
        assertThat(popularCategories.get(0).path("category").asText()).isEqualTo(BookCategoryDTO.COOKING.getProperty());
        assertThat(popularCategories.get(0).path("books").asLong()).isEqualTo(3);
        assertThat(popularCategories.get(1).path("category").asText()).isEqualTo(BookCategoryDTO.POETRY.getProperty());
        assertThat(popularCategories.get(1).path("books").asLong()).isEqualTo(2);
        assertThat(popularCategories.toString()).doesNotContain("Travel");
        assertThat(popularCategories.toString()).doesNotContain("Biography");
        assertThat(popularCategories.toString()).doesNotContain(REMOVED_CATEGORY_VALUE);
    }

    @Test
    void shouldReturnLocalizedCitySuggestions_whenCityAutocompleteIsRequested() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(MetadataPaths.METADATA_PATH_CITIES)
                        .queryParam("query", "mun")
                        .queryParam("limit", "5")
                        .header("Accept-Language", "de")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode cities = body.path("data");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(cities.isArray()).isTrue();
        assertThat(cities).anySatisfy(city -> {
            assertThat(city.path("value").asText()).isEqualTo("Munich");
            assertThat(city.path("label").asText()).isEqualTo("München");
        });
    }

    @Test
    void shouldReturnBadRequest_whenCityAutocompleteQueryIsTooShort() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(MetadataPaths.METADATA_PATH_CITIES)
                        .queryParam("query", "m")
                        .queryParam("limit", "5")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertValidationErrorResponse(responseBody(mvcResult), MetadataPaths.METADATA_PATH_CITIES);
    }

    private void saveBook(User owner, int bookNumber, String category, boolean exchanged, boolean deleted) {
        Book book = new Book();
        book.setName("Book " + bookNumber);
        book.setDescription("Description " + bookNumber);
        book.setAuthor("Author " + bookNumber);
        book.setCategory(category);
        book.setPublicationYear(2024);
        book.setCity("City " + bookNumber);
        book.setContactDetails("contact-" + bookNumber + "@test.com");
        book.setIsGift(false);
        book.setIsExchanged(exchanged);
        book.setUser(owner);

        if (deleted) {
            book.setDeletedAt(Instant.now());
        }

        bookRepository.save(book);
    }
}
