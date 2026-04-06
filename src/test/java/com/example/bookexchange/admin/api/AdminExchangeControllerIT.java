package com.example.bookexchange.admin.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.support.fixture.BookFixtureSupport;
import com.example.bookexchange.support.fixture.ExchangeFixtureSupport;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class AdminExchangeControllerIT extends IntegrationTestSupport {

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
    void shouldReturnFilteredExchanges_whenAdminGetsExchangesByStatus() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminExchange(1));
        Exchange pendingExchange = createExchangeWithStatus(FixtureNumbers.adminExchange(2), ExchangeStatus.PENDING);
        createExchangeWithStatus(FixtureNumbers.adminExchange(10), ExchangeStatus.APPROVED);

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_EXCHANGES)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("exchangeStatuses", ExchangeStatus.PENDING.name())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode content = body.path("data").path("content");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(pendingExchange.getId());
        assertThat(content.get(0).path("status").asText()).isEqualTo(ExchangeStatus.PENDING.name());
    }

    @Test
    void shouldReturnExchange_whenAdminGetsExchangeById() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminExchange(20));
        Exchange approvedExchange = createExchangeWithStatus(FixtureNumbers.adminExchange(21), ExchangeStatus.APPROVED);

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_EXCHANGES_ID, approvedExchange.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(approvedExchange.getId());
        assertThat(body.path("data").path("status").asText()).isEqualTo(ExchangeStatus.APPROVED.name());
        assertThat(body.path("data").path("senderUser").path("id").asLong()).isEqualTo(approvedExchange.getSenderUser().getId());
        assertThat(body.path("data").path("receiverUser").path("id").asLong()).isEqualTo(approvedExchange.getReceiverUser().getId());
    }

    @Test
    void shouldReturnNotFound_whenAdminGetsMissingExchangeById() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminExchange(30));

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_EXCHANGES_ID, Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.ADMIN_EXCHANGE_NOT_FOUND,
                AdminPaths.ADMIN_PATH_EXCHANGES + "/" + Long.MAX_VALUE
        );
    }

    private Exchange createExchangeWithStatus(int seed, ExchangeStatus status) {
        User sender = userUtil.createUser(seed);
        User receiver = userUtil.createUser(seed + 1);
        Long senderBookId = bookUtil.createBook(sender.getId(), seed);
        Long receiverBookId = bookUtil.createBook(receiver.getId(), seed + 1);
        Long exchangeId = exchangeUtilIT.createExchange(sender.getId(), receiver.getId(), senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).orElseThrow();
        exchange.setStatus(status);

        if (status == ExchangeStatus.APPROVED) {
            Book senderBook = bookRepository.findById(senderBookId).orElseThrow();
            Book receiverBook = bookRepository.findById(receiverBookId).orElseThrow();
            senderBook.setIsExchanged(true);
            receiverBook.setIsExchanged(true);
        }

        if (status == ExchangeStatus.DECLINED) {
            exchange.setDeclinerUser(receiver);
        }

        return exchangeRepository.save(exchange);
    }
}
