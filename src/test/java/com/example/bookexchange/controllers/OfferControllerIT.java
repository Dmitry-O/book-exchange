package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.bookexchange.util.TestConstants.*;

@Testcontainers
class OfferControllerIT extends AbstractIT {

    @Autowired
    private OfferController offerController;

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
    void getUserOffers() {
        exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        Page<ExchangeDTO> exchangeDTOs = offerController.getUserOffers(receiverUserId, PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangeDTOs.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getUserOffersNotFound() {
        Page<ExchangeDTO> exchangeDTOs = offerController.getUserOffers(receiverUserId, PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangeDTOs.getTotalElements()).isEqualTo(0);
    }

    @Rollback
    @Transactional
    @Test
    void getUserOfferDetails() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        ExchangeDetailsDTO exchangeDetailsDTO = offerController.getUserOfferDetails(receiverUserId, exchangeId);

        assertThat(exchangeDetailsDTO).isNotNull();
    }

    @Test
    void getUserRequestDetailsNotFound() {
        assertThrows(EntityNotFoundException.class, () -> offerController.getUserOfferDetails(System.nanoTime(), System.nanoTime()));
    }

    @Rollback
    @Transactional
    @Test
    void approveUserOffer() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        ResponseEntity responseEntity = offerController.approveUserOffer(receiverUserId, exchangeId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.APPROVED);
    }

    @Rollback
    @Transactional
    @Test
    void approveUserOfferUserNotFound() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        assertThrows(EntityNotFoundException.class, () -> offerController.approveUserOffer(System.nanoTime(), exchangeId));
    }

    @Test
    void approveUserOfferExchangeNotFound() {
        assertThrows(EntityNotFoundException.class, () -> offerController.approveUserOffer(receiverUserId, System.nanoTime()));
    }

    @Rollback
    @Transactional
    @Test
    void approveUserOfferExchangeStatusDeclined() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.DECLINED);
        exchangeRepository.save(exchange);

        assertThrows(IllegalStateException.class, () -> {
            offerController.approveUserOffer(receiverUserId, exchangeId);
        });
    }

    @Rollback
    @Transactional
    @Test
    void approveUserOfferExchangeStatusApproved() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(exchange);

        assertThrows(IllegalStateException.class, () -> {
            offerController.approveUserOffer(receiverUserId, exchangeId);
        });
    }

    @Rollback
    @Transactional
    @Test
    void declineUserOffer() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        ResponseEntity responseEntity = offerController.declineUserOffer(receiverUserId, exchangeId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(exchange.getDeclinerUser().getId()).isEqualTo(receiverUserId);
    }

    @Rollback
    @Transactional
    @Test
    void declineUserOfferUserNotFound() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        assertThrows(EntityNotFoundException.class, () -> offerController.declineUserOffer(System.nanoTime(), exchangeId));
    }

    @Test
    void declineUserOfferExchangeNotFound() {
        assertThrows(EntityNotFoundException.class, () -> offerController.declineUserOffer(receiverUserId, System.nanoTime()));
    }

    @Rollback
    @Transactional
    @Test
    void declineUserOfferExchangeStatusDeclined() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.DECLINED);
        exchangeRepository.save(exchange);

        assertThrows(IllegalStateException.class, () -> {
            offerController.declineUserOffer(receiverUserId, exchangeId);
        });
    }

    @Rollback
    @Transactional
    @Test
    void declineUserOfferExchangeStatusApproved() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(exchange);

        assertThrows(IllegalStateException.class, () -> {
            offerController.declineUserOffer(receiverUserId, exchangeId);
        });
    }
}