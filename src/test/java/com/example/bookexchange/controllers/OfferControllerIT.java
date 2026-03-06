package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.User;
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
    void getUserOffers() {
        exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        Page<ExchangeDTO> exchangeDTOs = offerController.getUserOffers(receiverUser, PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangeDTOs.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getUserOffersNotFound() {
        Page<ExchangeDTO> exchangeDTOs = offerController.getUserOffers(receiverUser, PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangeDTOs.getTotalElements()).isEqualTo(0);
    }

    @Rollback
    @Transactional
    @Test
    void getUserOfferDetails() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        ExchangeDetailsDTO exchangeDetailsDTO = offerController.getUserOfferDetails(receiverUser, exchangeId);

        assertThat(exchangeDetailsDTO).isNotNull();
    }

    @Test
    void getUserRequestDetailsNotFound() {
        assertThrows(EntityNotFoundException.class, () -> offerController.getUserOfferDetails(new User(), System.nanoTime()));
    }

    @Rollback
    @Transactional
    @Test
    void approveUserOffer() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        ResponseEntity<String> responseEntity = offerController.approveUserOffer(receiverUser, exchangeId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.APPROVED);
    }

    @Rollback
    @Transactional
    @Test
    void approveUserOfferUserNotFound() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        assertThrows(EntityNotFoundException.class, () -> offerController.approveUserOffer(new User(), exchangeId));
    }

    @Test
    void approveUserOfferExchangeNotFound() {
        assertThrows(EntityNotFoundException.class, () -> offerController.approveUserOffer(receiverUser, System.nanoTime()));
    }

    @Rollback
    @Transactional
    @Test
    void approveUserOfferExchangeStatusDeclined() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.DECLINED);
        exchangeRepository.save(exchange);

        assertThrows(IllegalStateException.class, () -> offerController.approveUserOffer(receiverUser, exchangeId));
    }

    @Rollback
    @Transactional
    @Test
    void approveUserOfferExchangeStatusApproved() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(exchange);

        assertThrows(IllegalStateException.class, () -> offerController.approveUserOffer(receiverUser, exchangeId));
    }

    @Rollback
    @Transactional
    @Test
    void declineUserOffer() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        ResponseEntity responseEntity = offerController.declineUserOffer(receiverUser, exchangeId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(exchange.getDeclinerUser().getId()).isEqualTo(receiverUser.getId());
    }

    @Rollback
    @Transactional
    @Test
    void declineUserOfferUserNotFound() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        assertThrows(EntityNotFoundException.class, () -> offerController.declineUserOffer(new User(), exchangeId));
    }

    @Test
    void declineUserOfferExchangeNotFound() {
        assertThrows(EntityNotFoundException.class, () -> offerController.declineUserOffer(receiverUser, System.nanoTime()));
    }

    @Rollback
    @Transactional
    @Test
    void declineUserOfferExchangeStatusDeclined() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.DECLINED);
        exchangeRepository.save(exchange);

        assertThrows(IllegalStateException.class, () -> offerController.declineUserOffer(receiverUser, exchangeId));
    }

    @Rollback
    @Transactional
    @Test
    void declineUserOfferExchangeStatusApproved() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(exchange);

        assertThrows(IllegalStateException.class, () -> offerController.declineUserOffer(receiverUser, exchangeId));
    }
}