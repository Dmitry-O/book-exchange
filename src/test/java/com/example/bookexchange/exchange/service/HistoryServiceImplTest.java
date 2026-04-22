package com.example.bookexchange.exchange.service;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateQueryDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateReadStateDTO;
import com.example.bookexchange.exchange.mapper.ExchangeMapper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static com.example.bookexchange.common.result.ResultFactory.ok;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
        ExchangeHistoryDTO historyDto = mock(ExchangeHistoryDTO.class);
        PageQueryDTO queryDTO = UnitTestDataFactory.pageQuery(0, 20);

        when(exchangeRepository.findUserExchangeHistory(eq(sender.getId()), eq(ExchangeStatus.PENDING), any()))
                .thenReturn(new PageImpl<>(List.of(exchange)));
        when(exchangeMapper.exchangeToExchangeHistoryDto(exchange, UserExchangeRole.SENDER)).thenReturn(historyDto);

        Result<Page<ExchangeHistoryDTO>> result =
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
        ExchangeHistoryDetailsDTO detailsDto = mock(ExchangeHistoryDetailsDTO.class);

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
    void shouldReturnNullContactDetails_whenSenderGetsDeclinedExchangeHistoryDetails() {
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
                ExchangeStatus.DECLINED
        );
        ExchangeHistoryDetailsDTO detailsDto = mock(ExchangeHistoryDetailsDTO.class);

        when(exchangeUtil.identifyUserExchangeRole(sender.getId(), exchange.getId())).thenReturn(ok(UserExchangeRole.SENDER));
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
        when(exchangeRepository.saveAndFlush(exchange)).thenReturn(exchange);
        when(exchangeMapper.exchangeToExchangeHistoryDetailsDto(
                exchange,
                receiver.getId(),
                receiver.getNickname(),
                null,
                UserExchangeRole.SENDER
        )).thenReturn(detailsDto);

        Result<ExchangeHistoryDetailsDTO> result = historyService.getUserExchangeHistoryDetails(sender.getId(), exchange.getId());

        assertSuccess(result, HttpStatus.OK);
        verify(exchangeMapper).exchangeToExchangeHistoryDetailsDto(
                exchange,
                receiver.getId(),
                receiver.getNickname(),
                null,
                UserExchangeRole.SENDER
        );
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
        ExchangeHistoryDetailsDTO detailsDto = mock(ExchangeHistoryDetailsDTO.class);

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
        ExchangeUpdateDTO updateDto = new ExchangeUpdateDTO();
        PageQueryDTO queryDTO = UnitTestDataFactory.pageQuery(0, 20);

        when(exchangeRepository.findUpdatesForUserByReadState(eq(sender.getId()), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(exchange)));
        when(exchangeMapper.exchangeToExchangeUpdateDto(exchange, UserExchangeRole.SENDER)).thenReturn(updateDto);

        Result<Page<ExchangeUpdateDTO>> result =
                historyService.getUnreadExchangeUpdates(sender.getId(), queryDTO);

        assertSuccess(result, HttpStatus.OK);
        verify(exchangeMapper).exchangeToExchangeUpdateDto(exchange, UserExchangeRole.SENDER);
    }

    @Test
    void shouldMapAllUpdatesWithResolvedRole_whenUserGetsAllExchangeUpdates() {
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
                ExchangeStatus.DECLINED
        );
        ExchangeUpdateDTO updateDto = new ExchangeUpdateDTO();
        ExchangeUpdateQueryDTO queryDTO = new ExchangeUpdateQueryDTO();
        queryDTO.setPageIndex(0);
        queryDTO.setPageSize(20);
        queryDTO.setReadState(ExchangeUpdateReadStateDTO.ALL);

        when(exchangeRepository.findUpdatesForUser(eq(sender.getId()), any()))
                .thenReturn(new PageImpl<>(List.of(exchange)));
        when(exchangeMapper.exchangeToExchangeUpdateDto(exchange, UserExchangeRole.SENDER)).thenReturn(updateDto);

        Result<Page<ExchangeUpdateDTO>> result =
                historyService.getExchangeUpdates(sender.getId(), queryDTO);

        assertSuccess(result, HttpStatus.OK);
        verify(exchangeRepository).findUpdatesForUser(eq(sender.getId()), any());
        verify(exchangeMapper).exchangeToExchangeUpdateDto(exchange, UserExchangeRole.SENDER);
    }

    @Test
    void shouldMapReadUpdatesWithResolvedRole_whenUserGetsReadExchangeUpdates() {
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
        ExchangeUpdateDTO updateDto = new ExchangeUpdateDTO();
        ExchangeUpdateQueryDTO queryDTO = new ExchangeUpdateQueryDTO();
        queryDTO.setPageIndex(0);
        queryDTO.setPageSize(20);
        queryDTO.setReadState(ExchangeUpdateReadStateDTO.READ);

        when(exchangeRepository.findUpdatesForUserByReadState(eq(sender.getId()), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(exchange)));
        when(exchangeMapper.exchangeToExchangeUpdateDto(exchange, UserExchangeRole.SENDER)).thenReturn(updateDto);

        Result<Page<ExchangeUpdateDTO>> result =
                historyService.getExchangeUpdates(sender.getId(), queryDTO);

        assertSuccess(result, HttpStatus.OK);
        verify(exchangeRepository).findUpdatesForUserByReadState(eq(sender.getId()), eq(true), any());
        verify(exchangeMapper).exchangeToExchangeUpdateDto(exchange, UserExchangeRole.SENDER);
    }

    @Test
    void shouldReturnNullContactDetails_whenReceiverGetsGiftExchangeHistoryDetails() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        Exchange exchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                sender,
                receiver,
                null,
                receiverBook,
                ExchangeStatus.APPROVED
        );
        ExchangeHistoryDetailsDTO detailsDto = mock(ExchangeHistoryDetailsDTO.class);

        when(exchangeUtil.identifyUserExchangeRole(receiver.getId(), exchange.getId())).thenReturn(ok(UserExchangeRole.RECEIVER));
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
        when(exchangeRepository.saveAndFlush(exchange)).thenReturn(exchange);
        when(exchangeMapper.exchangeToExchangeHistoryDetailsDto(
                exchange,
                sender.getId(),
                sender.getNickname(),
                null,
                UserExchangeRole.RECEIVER
        )).thenReturn(detailsDto);

        Result<ExchangeHistoryDetailsDTO> result = historyService.getUserExchangeHistoryDetails(receiver.getId(), exchange.getId());

        assertSuccess(result, HttpStatus.OK);
        verify(exchangeMapper).exchangeToExchangeHistoryDetailsDto(
                exchange,
                sender.getId(),
                sender.getNickname(),
                null,
                UserExchangeRole.RECEIVER
        );
    }

    @Test
    void shouldReturnNullContactDetails_whenReceiverGetsDeclinedExchangeHistoryDetails() {
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
                ExchangeStatus.DECLINED
        );
        ExchangeHistoryDetailsDTO detailsDto = mock(ExchangeHistoryDetailsDTO.class);

        when(exchangeUtil.identifyUserExchangeRole(receiver.getId(), exchange.getId())).thenReturn(ok(UserExchangeRole.RECEIVER));
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
        when(exchangeRepository.saveAndFlush(exchange)).thenReturn(exchange);
        when(exchangeMapper.exchangeToExchangeHistoryDetailsDto(
                exchange,
                sender.getId(),
                sender.getNickname(),
                null,
                UserExchangeRole.RECEIVER
        )).thenReturn(detailsDto);

        Result<ExchangeHistoryDetailsDTO> result = historyService.getUserExchangeHistoryDetails(receiver.getId(), exchange.getId());

        assertSuccess(result, HttpStatus.OK);
        verify(exchangeMapper).exchangeToExchangeHistoryDetailsDto(
                exchange,
                sender.getId(),
                sender.getNickname(),
                null,
                UserExchangeRole.RECEIVER
        );
    }
}
