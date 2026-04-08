package com.example.bookexchange.exchange.service;

import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.exchange.dto.ExchangeUnreadUpdateDTO;
import com.example.bookexchange.exchange.mapper.ExchangeMapper;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.exchange.util.ExchangeUtil;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static com.example.bookexchange.common.result.ResultFactory.ok;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryServiceImplTest {

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private ExchangeUtil exchangeUtil;

    @Mock
    private ExchangeMapper exchangeMapper;

    @InjectMocks
    private HistoryServiceImpl historyService;

    @Test
    void shouldMapEntriesWithResolvedRole_whenUserGetsExchangeHistory() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        Exchange exchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                sender,
                receiver,
                senderBook,
                receiverBook,
                ExchangeStatus.APPROVED
        );
        ExchangeHistoryDTO historyDto = org.mockito.Mockito.mock(ExchangeHistoryDTO.class);
        PageQueryDTO queryDTO = UnitTestDataFactory.pageQuery(0, 20);

        when(exchangeRepository.findUserExchangeHistory(eq(sender.getId()), eq(ExchangeStatus.PENDING), any()))
                .thenReturn(new PageImpl<>(List.of(exchange)));
        when(exchangeMapper.exchangeToExchangeHistoryDto(exchange, UserExchangeRole.SENDER)).thenReturn(historyDto);

        Result<org.springframework.data.domain.Page<ExchangeHistoryDTO>> result =
                historyService.getUserExchangeHistory(sender.getId(), queryDTO);

        assertSuccess(result, HttpStatus.OK);
        verify(exchangeMapper).exchangeToExchangeHistoryDto(exchange, UserExchangeRole.SENDER);
    }

    @Test
    void shouldMarkSenderViewAsRead_whenSenderGetsUnreadExchangeHistoryDetails() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        Exchange exchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                sender,
                receiver,
                senderBook,
                receiverBook,
                ExchangeStatus.APPROVED
        );
        ExchangeHistoryDetailsDTO detailsDto = org.mockito.Mockito.mock(ExchangeHistoryDetailsDTO.class);

        when(exchangeUtil.identifyUserExchangeRole(sender.getId(), exchange.getId())).thenReturn(ok(UserExchangeRole.SENDER));
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
        when(exchangeRepository.saveAndFlush(exchange)).thenReturn(exchange);
        when(exchangeMapper.exchangeToExchangeHistoryDetailsDto(
                exchange,
                receiver.getId(),
                receiver.getNickname(),
                receiverBook.getContactDetails(),
                UserExchangeRole.SENDER
        )).thenReturn(detailsDto);

        Result<ExchangeHistoryDetailsDTO> result = historyService.getUserExchangeHistoryDetails(sender.getId(), exchange.getId());

        assertSuccess(result, HttpStatus.OK);
        assertThat(exchange.getIsReadBySender()).isTrue();
        verify(exchangeRepository).saveAndFlush(exchange);
    }

    @Test
    void shouldNotResaveExchange_whenReceiverGetsAlreadyReadExchangeHistoryDetails() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        Exchange exchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                sender,
                receiver,
                senderBook,
                receiverBook,
                ExchangeStatus.APPROVED
        );
        exchange.setIsReadByReceiver(true);
        ExchangeHistoryDetailsDTO detailsDto = org.mockito.Mockito.mock(ExchangeHistoryDetailsDTO.class);

        when(exchangeUtil.identifyUserExchangeRole(receiver.getId(), exchange.getId())).thenReturn(ok(UserExchangeRole.RECEIVER));
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
        when(exchangeMapper.exchangeToExchangeHistoryDetailsDto(
                exchange,
                sender.getId(),
                sender.getNickname(),
                senderBook.getContactDetails(),
                UserExchangeRole.RECEIVER
        )).thenReturn(detailsDto);

        Result<ExchangeHistoryDetailsDTO> result = historyService.getUserExchangeHistoryDetails(receiver.getId(), exchange.getId());

        assertSuccess(result, HttpStatus.OK);
        verify(exchangeRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldMapUnreadUpdatesWithResolvedRole_whenUserGetsUnreadExchangeUpdates() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        Exchange exchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                sender,
                receiver,
                senderBook,
                receiverBook,
                ExchangeStatus.PENDING
        );
        ExchangeUnreadUpdateDTO unreadDto = new ExchangeUnreadUpdateDTO();
        PageQueryDTO queryDTO = UnitTestDataFactory.pageQuery(0, 20);

        when(exchangeRepository.findUnreadUpdatesForUser(eq(sender.getId()), any()))
                .thenReturn(new PageImpl<>(List.of(exchange)));
        when(exchangeMapper.exchangeToExchangeUnreadUpdateDto(exchange, UserExchangeRole.SENDER)).thenReturn(unreadDto);

        Result<org.springframework.data.domain.Page<ExchangeUnreadUpdateDTO>> result =
                historyService.getUnreadExchangeUpdates(sender.getId(), queryDTO);

        assertSuccess(result, HttpStatus.OK);
        verify(exchangeMapper).exchangeToExchangeUnreadUpdateDto(exchange, UserExchangeRole.SENDER);
    }
}
