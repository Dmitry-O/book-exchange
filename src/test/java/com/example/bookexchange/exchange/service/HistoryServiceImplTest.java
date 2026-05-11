package com.example.bookexchange.exchange.service;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.audit.service.SoftDeleteFilterHelper;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.notification.UserUpdate;
import com.example.bookexchange.common.notification.UserUpdateRepository;
import com.example.bookexchange.common.notification.UserUpdateType;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateQueryDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateReadStateChangeDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateReadStateDTO;
import com.example.bookexchange.exchange.mapper.ExchangeMapper;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.exchange.util.ExchangeUtil;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
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
    private UserUpdateRepository userUpdateRepository;

    @Mock
    private ExchangeUtil exchangeUtil;

    @Mock
    private ExchangeMapper exchangeMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SoftDeleteFilterHelper softDeleteFilterHelper;

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
    void shouldMergeExchangeAndNotificationUpdatesInOneFeed() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Exchange exchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                sender,
                receiver,
                UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender),
                UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver),
                ExchangeStatus.PENDING
        );
        ExchangeUpdateDTO exchangeUpdateDto = new ExchangeUpdateDTO();
        exchangeUpdateDto.setId(exchange.getId());
        exchangeUpdateDto.setExchangeId(exchange.getId());
        exchangeUpdateDto.setUpdateCreatedAt(java.time.Instant.parse("2026-04-24T10:00:00Z"));
        UserUpdate notification = new UserUpdate();
        notification.setId(77L);
        notification.setUser(sender);
        notification.setType(UserUpdateType.ADMIN_BOOK_UPDATED);
        notification.setIsRead(false);
        notification.setBookId(UnitFixtureIds.SENDER_BOOK_ID);
        notification.setBookName("Updated book");
        notification.setCreatedAt(java.time.Instant.parse("2026-04-24T12:00:00Z"));
        ExchangeUpdateQueryDTO queryDTO = new ExchangeUpdateQueryDTO();
        queryDTO.setPageIndex(0);
        queryDTO.setPageSize(20);
        queryDTO.setReadState(ExchangeUpdateReadStateDTO.ALL);

        when(exchangeRepository.findUpdatesForUser(eq(sender.getId()), any()))
                .thenReturn(new PageImpl<>(List.of(exchange)));
        when(userUpdateRepository.findForUser(eq(sender.getId()), any()))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(exchangeMapper.exchangeToExchangeUpdateDto(exchange, UserExchangeRole.SENDER)).thenReturn(exchangeUpdateDto);

        Result<Page<ExchangeUpdateDTO>> result = historyService.getExchangeUpdates(sender.getId(), queryDTO);

        var success = assertSuccess(result, HttpStatus.OK);
        assertThat(success.body().getContent()).extracting(ExchangeUpdateDTO::getUpdateType)
                .containsExactly(UserUpdateType.ADMIN_BOOK_UPDATED, UserUpdateType.EXCHANGE);
        assertThat(success.body().getTotalElements()).isEqualTo(2);
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
    void shouldUpdateReadStateForReceiver_whenUserTogglesUpdateReadState() {
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
        ExchangeUpdateReadStateChangeDTO dto = new ExchangeUpdateReadStateChangeDTO();
        dto.setIsRead(true);

        when(exchangeUtil.identifyUserExchangeRole(receiver.getId(), exchange.getId())).thenReturn(ok(UserExchangeRole.RECEIVER));
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
        when(exchangeRepository.saveAndFlush(exchange)).thenReturn(exchange);
        when(exchangeMapper.exchangeToExchangeUpdateDto(exchange, UserExchangeRole.RECEIVER)).thenReturn(updateDto);

        Result<ExchangeUpdateDTO> result = historyService.updateExchangeUpdateReadState(receiver.getId(), exchange.getId(), dto);

        assertSuccess(result, HttpStatus.OK);
        assertThat(exchange.getIsReadByReceiver()).isTrue();
        assertThat(exchange.getUpdateCreatedAt()).isNotNull();
        verify(exchangeRepository).saveAndFlush(exchange);
        verify(exchangeMapper).exchangeToExchangeUpdateDto(exchange, UserExchangeRole.RECEIVER);
    }

    @Test
    void shouldNotPersistWhenReadStateAlreadyMatches() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Exchange exchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                sender,
                receiver,
                UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender),
                UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver),
                ExchangeStatus.PENDING
        );
        exchange.setIsReadByReceiver(true);
        ExchangeUpdateDTO updateDto = new ExchangeUpdateDTO();
        ExchangeUpdateReadStateChangeDTO dto = new ExchangeUpdateReadStateChangeDTO();
        dto.setIsRead(true);

        when(exchangeUtil.identifyUserExchangeRole(receiver.getId(), exchange.getId())).thenReturn(ok(UserExchangeRole.RECEIVER));
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
        when(exchangeMapper.exchangeToExchangeUpdateDto(exchange, UserExchangeRole.RECEIVER)).thenReturn(updateDto);

        Result<ExchangeUpdateDTO> result = historyService.updateExchangeUpdateReadState(receiver.getId(), exchange.getId(), dto);

        assertSuccess(result, HttpStatus.OK);
        verify(exchangeRepository, never()).saveAndFlush(any());
        verify(exchangeMapper).exchangeToExchangeUpdateDto(exchange, UserExchangeRole.RECEIVER);
    }

    @Test
    void shouldUpdateReadStateForNotificationUpdate() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        UserUpdate notification = new UserUpdate();
        notification.setId(77L);
        notification.setUser(user);
        notification.setType(UserUpdateType.REPORT_RESOLVED);
        notification.setIsRead(false);
        notification.setTargetUrl("/app/my-reports");
        notification.setCreatedAt(java.time.Instant.parse("2026-04-24T12:00:00Z"));
        ExchangeUpdateReadStateChangeDTO dto = new ExchangeUpdateReadStateChangeDTO();
        dto.setIsRead(true);

        when(userUpdateRepository.findByIdAndUserId(notification.getId(), user.getId()))
                .thenReturn(Optional.of(notification));
        when(userUpdateRepository.saveAndFlush(notification)).thenReturn(notification);

        Result<ExchangeUpdateDTO> result = historyService.updateNotificationUpdateReadState(user.getId(), notification.getId(), dto);

        var success = assertSuccess(result, HttpStatus.OK);
        assertThat(notification.getIsRead()).isTrue();
        assertThat(success.body().getNotificationId()).isEqualTo(notification.getId());
        assertThat(success.body().getUpdateType()).isEqualTo(UserUpdateType.REPORT_RESOLVED);
        verify(userUpdateRepository).saveAndFlush(notification);
    }

    @Test
    void shouldUseAnonymizedNicknameForDeletedReportTargetUserInUpdates() {
        User viewer = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        User deletedTargetUser = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "anonymized-1104");
        deletedTargetUser.setDeletedAt(java.time.Instant.parse("2026-04-24T08:15:30Z"));
        deletedTargetUser.setPhotoUrl(null);

        UserUpdate notification = new UserUpdate();
        notification.setId(88L);
        notification.setUser(viewer);
        notification.setType(UserUpdateType.REPORT_RESOLVED);
        notification.setIsRead(false);
        notification.setTargetUrl("/app/my-reports");
        notification.setCreatedAt(java.time.Instant.parse("2026-04-24T12:00:00Z"));
        notification.setReportTargetType(TargetType.USER);
        notification.setTargetUserId(deletedTargetUser.getId());
        notification.setTargetUserNickname("test_user112");
        notification.setTargetUserPhotoUrl(null);

        ExchangeUpdateQueryDTO queryDTO = new ExchangeUpdateQueryDTO();
        queryDTO.setPageIndex(0);
        queryDTO.setPageSize(20);
        queryDTO.setReadState(ExchangeUpdateReadStateDTO.ALL);

        when(exchangeRepository.findUpdatesForUser(eq(viewer.getId()), any()))
                .thenReturn(Page.empty());
        when(userUpdateRepository.findForUser(eq(viewer.getId()), any()))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(softDeleteFilterHelper.runWithoutDeletedFilter(any())).thenAnswer(invocation -> invocation.<java.util.function.Supplier<?>>getArgument(0).get());
        when(userRepository.findById(deletedTargetUser.getId())).thenReturn(Optional.of(deletedTargetUser));

        Result<Page<ExchangeUpdateDTO>> result = historyService.getExchangeUpdates(viewer.getId(), queryDTO);

        var success = assertSuccess(result, HttpStatus.OK);
        assertThat(success.body().getContent()).hasSize(1);
        assertThat(success.body().getContent().getFirst().getTargetUserNickname()).isEqualTo("anonymized-1104");
        assertThat(success.body().getContent().getFirst().getTargetUserPhotoUrl()).isNull();
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
