package com.example.bookexchange.common.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class MetadataControllerIT extends IntegrationTestSupport {

    private MockMvc mockMvc;

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

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(categories.isArray()).isTrue();
        assertThat(categories.toString()).contains("Drama");
        assertThat(categories.toString()).contains("Fantasy");
        assertThat(categories.toString()).contains("Science Fiction");
        assertThat(categories.toString()).contains("Other");
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
}
