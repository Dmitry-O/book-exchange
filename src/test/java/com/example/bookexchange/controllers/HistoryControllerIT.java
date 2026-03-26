package com.example.bookexchange.controllers;

import com.example.bookexchange.exchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.exchange.api.HistoryController;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.util.BookUtil;
import com.example.bookexchange.util.ExchangeUtilIT;
import com.example.bookexchange.util.UserUtil;
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
    void getExchangeHistory() {
        Long approvedSenderBookId = bookUtil.createBook(senderUser.getId(), 3);
        Long approvedReceiverBookId = bookUtil.createBook(receiverUser.getId(), 4);
        Long declinedSenderBookId = bookUtil.createBook(senderUser.getId(), 5);
        Long declinedReceiverBookId = bookUtil.createBook(receiverUser.getId(), 6);

        exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);
        Long approvedExchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), approvedSenderBookId, approvedReceiverBookId);
        Long declinedExchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), declinedSenderBookId, declinedReceiverBookId);

        Exchange approvedExchange = exchangeRepository.findById(approvedExchangeId).get();
        Exchange declinedExchange = exchangeRepository.findById(declinedExchangeId).get();

        User declinerUser = userRepository.findById(senderUser.getId()).get();

        approvedExchange.setStatus(ExchangeStatus.APPROVED);
        declinedExchange.setStatus(ExchangeStatus.DECLINED);
        declinedExchange.setDeclinerUser(declinerUser);

        exchangeRepository.save(approvedExchange);
        exchangeRepository.save(declinedExchange);

        Page<ExchangeHistoryDTO> exchangeHistoryDTOs = historyController.getExchangeHistory(senderUser.getId(), PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangeHistoryDTOs.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getExchangeHistoryNotFound() {
        Page<ExchangeHistoryDTO> exchangeHistoryDTOs = historyController.getExchangeHistory(senderUser.getId(), PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangeHistoryDTOs.getTotalElements()).isEqualTo(0);
    }

    @Rollback
    @Transactional
    @Test
    void getExchangeHistoryDetails() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();

        exchange.setStatus(ExchangeStatus.APPROVED);

        exchangeRepository.save(exchange);

        ExchangeHistoryDetailsDTO exchangeHistoryDetailsDTO = historyController.getExchangeHistoryDetails(senderUser.getId(), exchangeId);

        assertThat(exchangeHistoryDetailsDTO).isNotNull();
    }

    @Rollback
    @Transactional
    @Test
    void getExchangeHistoryDetailsUserNotFound() {
        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        assertThrows(NotFoundException.class, () -> historyController.getExchangeHistoryDetails(System.nanoTime(), exchangeId));
    }

    @Test
    void getExchangeHistoryDetailsExchangeNotFound() {
        assertThrows(NotFoundException.class, () -> historyController.getExchangeHistoryDetails(senderUser.getId(), System.nanoTime()));
    }
}