package com.example.bookexchange.controllers;

import com.example.bookexchange.exchange.dto.ExchangeDTO;
import com.example.bookexchange.exchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.exchange.dto.RequestCreateDTO;
import com.example.bookexchange.exchange.api.RequestController;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.util.BookUtil;
import com.example.bookexchange.util.ExchangeUtilIT;
import com.example.bookexchange.util.UserUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.bookexchange.util.TestConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class RequestControllerIT extends AbstractIT {

    @Autowired
    private RequestController requestController;

    @Autowired
    private ExchangeRepository exchangeRepository;

    @Autowired
    private UserUtil userUtil;

    @Autowired
    private BookUtil bookUtil;

    @Autowired
    private ExchangeUtilIT exchangeUtilIT;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    ObjectMapper objectMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private User senderUser;
    private User receiverUser;
    private Long senderBookId;
    private Long receiverBookId;

    @BeforeAll
    void init() {
        senderUser = userUtil.createUser(1);
        receiverUser = userUtil.createUser(2);

        senderBookId = bookUtil.createBook(senderUser.getId(), 1);
        receiverBookId = bookUtil.createBook(receiverUser.getId(), 2);
    }

    @AfterAll
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            bookUtil.deleteUserBooks(senderUser.getId());
            bookUtil.deleteUserBooks(receiverUser.getId());
            userUtil.deleteUser(senderUser.getId());
            userUtil.deleteUser(receiverUser.getId());
        });

        senderUser = null;
        receiverUser = null;
        senderBookId = null;
        receiverBookId = null;
    }

    @Rollback
    @Transactional
    @Test
    void createRequest() {
        RequestCreateDTO requestCreateDTO = RequestCreateDTO.builder()
                .receiverUserId(receiverUser.getId())
                .senderBookId(senderBookId)
                .receiverBookId(receiverBookId)
                .build();

        ResponseEntity<String> responseEntity = requestController.createRequest(senderUser.getId(), requestCreateDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
        assertThat(responseEntity.getHeaders().getLocation()).isNotNull();

        String[] locationId = responseEntity.getHeaders().getLocation().getPath().split("/");
        Long savedId = Long.valueOf(locationId[4]);

        Exchange exchange = exchangeRepository.findById(savedId).get();

        assertThat(exchange).isNotNull();
    }

    @Rollback
    @Transactional
    @Test
    void createRequestBadRequest() throws Exception {
        RequestCreateDTO requestCreateDTO = RequestCreateDTO.builder().build();

        MvcResult mvcResult = mockMvc.perform(post(RequestController.REQUEST_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestCreateDTO))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()", is(3)))
                .andReturn();

        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Rollback
    @Transactional
    @Test
    void getUserRequestDetails() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        ExchangeDetailsDTO exchangeDTO = requestController.getUserRequestDetails(senderUser.getId(), exchangeId);

        assertThat(exchangeDTO).isNotNull();
    }

    @Test
    void getUserRequestDetailsNotFound() {
        assertThrows(NotFoundException.class, () -> requestController.getUserRequestDetails(System.nanoTime(), System.nanoTime()));
    }

    @Rollback
    @Transactional
    @Test
    void getUserRequests() {
        exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        Page<ExchangeDTO> exchangeDTOs = requestController.getUserRequests(senderUser.getId(), PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangeDTOs.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getUserRequestsNotFound() {
        Page<ExchangeDTO> exchangeDTOs = requestController.getUserRequests(senderUser.getId(), PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangeDTOs.getTotalElements()).isEqualTo(0);
    }

    @Rollback
    @Transactional
    @Test
    void declineUserRequest() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        ResponseEntity<String> responseEntity = requestController.declineUserRequest(senderUser.getId(), exchangeId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(exchange.getDeclinerUser().getId()).isEqualTo(senderUser.getId());
    }

    @Rollback
    @Transactional
    @Test
    void declineUserRequestUserNotFound() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        assertThrows(NotFoundException.class, () -> requestController.declineUserRequest(System.nanoTime(), exchangeId));
    }

    @Test
    void declineUserRequestExchangeNotFound() {
        assertThrows(NotFoundException.class, () -> requestController.declineUserRequest(senderUser.getId(), System.nanoTime()));
    }

    @Rollback
    @Transactional
    @Test
    void declineUserRequestExchangeStatusDeclined() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.DECLINED);
        exchangeRepository.save(exchange);

        assertThrows(BadRequestException.class, () -> requestController.declineUserRequest(senderUser.getId(), exchangeId));
    }

    @Rollback
    @Transactional
    @Test
    void declineUserRequestExchangeStatusApproved() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(exchange);

        assertThrows(BadRequestException.class, () -> requestController.declineUserRequest(senderUser.getId(), exchangeId));
    }
}