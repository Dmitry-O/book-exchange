package com.example.bookexchange.controllers;

import com.example.bookexchange.config.TestUserConfig;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.repositories.ExchangeRepository;
import com.example.bookexchange.util.BookUtil;
import com.example.bookexchange.util.ExchangeUtilIT;
import com.example.bookexchange.util.UserUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.bookexchange.util.TestConstants.*;

@SpringBootTest
@Import(TestUserConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestControllerIT {

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

    private Long senderUserId;
    private Long receiverUserId;
    private Long senderBookId;
    private Long receiverBookId;

    @BeforeAll
    void init() {
        senderUserId = userUtil.createUser(1);
        receiverUserId = userUtil.createUser(2);

        senderBookId = bookUtil.createBook(senderUserId, 1);
        receiverBookId = bookUtil.createBook(receiverUserId, 2);
    }

    @AfterAll
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            bookUtil.deleteUserBooks(senderUserId);
            bookUtil.deleteUserBooks(receiverUserId);
            userUtil.deleteUser(senderUserId);
            userUtil.deleteUser(receiverUserId);
        });

        senderUserId = null;
        receiverUserId = null;
        senderBookId = null;
        receiverBookId = null;
    }

    @Rollback
    @Transactional
    @Test
    void createRequest() {
        RequestCreateDTO requestCreateDTO = RequestCreateDTO.builder()
                .senderUserId(senderUserId)
                .receiverUserId(receiverUserId)
                .senderBookId(senderBookId)
                .receiverBookId(receiverBookId)
                .build();

        ResponseEntity responseEntity = requestController.createRequest(requestCreateDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
        assertThat(responseEntity.getHeaders().getLocation()).isNotNull();

        String[] locationId = responseEntity.getHeaders().getLocation().getPath().split("/");
        Long savedId = Long.valueOf(locationId[5]);

        Exchange exchange = exchangeRepository.findById(savedId).get();

        assertThat(exchange).isNotNull();
    }

    @Rollback
    @Transactional
    @Test
    void getUserRequestDetails() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        ExchangeDetailsDTO exchangeDTO = requestController.getUserRequestDetails(senderUserId, exchangeId);

        assertThat(exchangeDTO).isNotNull();
    }

    @Test
    void getUserRequestDetailsNotFound() {
        assertThrows(EntityNotFoundException.class, () -> requestController.getUserRequestDetails(System.nanoTime(), System.nanoTime()));
    }

    @Rollback
    @Transactional
    @Test
    void getUserRequests() {
        exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        Page<ExchangeDTO> exchangeDTOs = requestController.getUserRequests(senderUserId, PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangeDTOs.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getUserRequestsNotFound() {
        Page<ExchangeDTO> exchangeDTOs = requestController.getUserRequests(senderUserId, PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangeDTOs.getTotalElements()).isEqualTo(0);
    }

    @Rollback
    @Transactional
    @Test
    void declineUserRequest() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        ResponseEntity responseEntity = requestController.declineUserRequest(senderUserId, exchangeId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(exchange.getDeclinerUser().getId()).isEqualTo(senderUserId);
    }

    @Rollback
    @Transactional
    @Test
    void declineUserRequestUserNotFound() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        assertThrows(EntityNotFoundException.class, () -> requestController.declineUserRequest(System.nanoTime(), exchangeId));
    }

    @Test
    void declineUserRequestExchangeNotFound() {
        assertThrows(EntityNotFoundException.class, () -> requestController.declineUserRequest(senderUserId, System.nanoTime()));
    }

    @Rollback
    @Transactional
    @Test
    void declineUserRequestExchangeStatusDeclined() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.DECLINED);
        exchangeRepository.save(exchange);

        assertThrows(IllegalStateException.class, () -> {
            requestController.declineUserRequest(senderUserId, exchangeId);
        });
    }

    @Rollback
    @Transactional
    @Test
    void declineUserRequestExchangeStatusApproved() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(exchange);

        assertThrows(IllegalStateException.class, () -> {
            requestController.declineUserRequest(senderUserId, exchangeId);
        });
    }
}