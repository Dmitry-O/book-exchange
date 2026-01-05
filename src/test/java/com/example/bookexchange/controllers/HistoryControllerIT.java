package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.ExchangeRepository;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.util.BookUtil;
import com.example.bookexchange.util.ExchangeUtilIT;
import com.example.bookexchange.util.UserUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.bookexchange.util.TestConstants.*;

@Testcontainers
class HistoryControllerIT extends AbstractIT {

    @Autowired
    private HistoryController historyController;

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
    private UserRepository userRepository;

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
    void getExchangeHistory() {
        Long approvedSenderBookId = bookUtil.createBook(senderUserId, 3);
        Long approvedReceiverBookId = bookUtil.createBook(receiverUserId, 4);
        Long declinedSenderBookId = bookUtil.createBook(senderUserId, 5);
        Long declinedReceiverBookId = bookUtil.createBook(receiverUserId, 6);

        exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);
        Long approvedExchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, approvedSenderBookId, approvedReceiverBookId);
        Long declinedExchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, declinedSenderBookId, declinedReceiverBookId);

        Exchange approvedExchange = exchangeRepository.findById(approvedExchangeId).get();
        Exchange declinedExchange = exchangeRepository.findById(declinedExchangeId).get();

        User declinerUser = userRepository.findById(senderUserId).get();

        approvedExchange.setStatus(ExchangeStatus.APPROVED);
        declinedExchange.setStatus(ExchangeStatus.DECLINED);
        declinedExchange.setDeclinerUser(declinerUser);

        exchangeRepository.save(approvedExchange);
        exchangeRepository.save(declinedExchange);

        Page<ExchangeHistoryDTO> exchangeHistoryDTOs = historyController.getExchangeHistory(senderUserId, PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangeHistoryDTOs.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getExchangeHistoryNotFound() {
        Page<ExchangeHistoryDTO> exchangeHistoryDTOs = historyController.getExchangeHistory(senderUserId, PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangeHistoryDTOs.getTotalElements()).isEqualTo(0);
    }

    @Rollback
    @Transactional
    @Test
    void getExchangeHistoryDetails() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.APPROVED);

        exchangeRepository.save(exchange);

        ExchangeHistoryDetailsDTO exchangeHistoryDetailsDTO = historyController.getExchangeHistoryDetails(senderUserId, exchangeId);

        assertThat(exchangeHistoryDetailsDTO).isNotNull();
    }

    @Rollback
    @Transactional
    @Test
    void getExchangeHistoryDetailsUserNotFound() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        assertThrows(EntityNotFoundException.class, () -> {
            historyController.getExchangeHistoryDetails(System.nanoTime(), exchangeId);
        });
    }

    @Test
    void getExchangeHistoryDetailsExchangeNotFound() {
        assertThrows(EntityNotFoundException.class, () -> {
            historyController.getExchangeHistoryDetails(senderUserId, System.nanoTime());
        });
    }
}