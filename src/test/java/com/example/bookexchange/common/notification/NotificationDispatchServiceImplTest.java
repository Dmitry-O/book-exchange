package com.example.bookexchange.common.notification;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.email.EmailService;
import com.example.bookexchange.common.email.NotificationEmailRequest;
import com.example.bookexchange.common.audit.service.SoftDeleteFilterHelper;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.report.dto.ReportTargetBookDTO;
import com.example.bookexchange.report.dto.ReportTargetUserDTO;
import com.example.bookexchange.report.mapper.ReportMapper;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.report.repository.ReportRepository;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.example.bookexchange.common.result.ResultFactory.successVoid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceImplTest {

    @Mock
    private EmailService emailService;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserUpdateRepository userUpdateRepository;

    @Mock
    private SoftDeleteFilterHelper softDeleteFilterHelper;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private NotificationDisplayLocalizer notificationDisplayLocalizer;

    private NotificationDispatchServiceImpl notificationDispatchService;

    @BeforeEach
    void setUp() {
        lenient().when(notificationDisplayLocalizer.localizeBookCategory(anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(notificationDisplayLocalizer.localizeCity(anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationDispatchService = new NotificationDispatchServiceImpl(
                emailService,
                bookRepository,
                userRepository,
                reportRepository,
                userUpdateRepository,
                softDeleteFilterHelper,
                reportMapper,
                notificationDisplayLocalizer,
                messageSource(),
                appProperties(),
                Runnable::run
        );
    }

    @Test
    void shouldSendDifferentEmailsToSenderAndReceiver_whenExchangeIsCreated() {
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
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        notificationDispatchService.sendExchangeCreatedNotifications(exchange);

        verify(emailService, times(2)).sendNotificationEmail(requestCaptor.capture());
        List<NotificationEmailRequest> requests = requestCaptor.getAllValues();
        assertThat(requests).extracting(NotificationEmailRequest::getEmailTo)
                .containsExactlyInAnyOrder("sender@example.com", "receiver@example.com");
        assertThat(requests).extracting(NotificationEmailRequest::getSubject)
                .containsExactlyInAnyOrder("Exchange request sent", "New exchange request received");
        assertThat(requests).allSatisfy(request -> {
            assertThat(request.getHighlights()).isEmpty();
            assertThat(request.getEventTime()).isNotBlank();
            assertThat(request.getDetails()).isEmpty();
            assertThat(request.getExchange()).isNotNull();
            assertThat(request.getExchange().getReferenceText()).isNull();
            assertThat(request.getExchange().getLeftBook()).isNotNull();
            assertThat(request.getExchange().getRightBook()).isNotNull();
            assertThat(request.getExchange().getLeftUser()).isNotNull();
            assertThat(request.getExchange().getRightUser()).isNotNull();
        });
    }

    @Test
    void shouldSendDifferentEmailsToSenderAndReceiver_whenExchangeIsApproved() {
        Exchange exchange = standardExchange(ExchangeStatus.APPROVED);

        assertExchangeNotifications(
                () -> notificationDispatchService.sendExchangeApprovedNotifications(exchange),
                "Your exchange request was approved",
                "You approved the exchange request"
        );
    }

    @Test
    void shouldSendDifferentEmailsToSenderAndReceiver_whenSenderDeclinesExchange() {
        Exchange exchange = standardExchange(ExchangeStatus.DECLINED);
        exchange.setDeclinerUser(exchange.getSenderUser());

        assertExchangeNotifications(
                () -> notificationDispatchService.sendExchangeDeclinedBySenderNotifications(exchange),
                "You canceled your exchange request",
                "Exchange request was canceled by sender"
        );
    }

    @Test
    void shouldSendDifferentEmailsToSenderAndReceiver_whenReceiverDeclinesExchange() {
        Exchange exchange = standardExchange(ExchangeStatus.DECLINED);
        exchange.setDeclinerUser(exchange.getReceiverUser());

        assertExchangeNotifications(
                () -> notificationDispatchService.sendExchangeDeclinedByReceiverNotifications(exchange),
                "Your exchange request was declined",
                "You declined the exchange request"
        );
    }

    @Test
    void shouldSendDifferentEmailsToSenderAndReceiver_whenExchangeIsAutoDeclined() {
        Exchange exchange = standardExchange(ExchangeStatus.DECLINED);

        assertExchangeNotifications(
                () -> notificationDispatchService.sendExchangeAutoDeclinedNotifications(List.of(exchange)),
                "Your exchange request was automatically declined",
                "An exchange offer was automatically declined"
        );
    }

    @Test
    void shouldSkipDeletedRecipientWhenExchangeIsAutoDeclined() {
        Exchange exchange = standardExchange(ExchangeStatus.DECLINED);
        exchange.getSenderUser().setDeletedAt(Instant.parse("2026-04-24T10:15:30Z"));
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        notificationDispatchService.sendExchangeAutoDeclinedNotifications(List.of(exchange));

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getEmailTo()).isEqualTo(exchange.getReceiverUser().getEmail());
    }

    @Test
    void shouldContinueSendingExchangeNotifications_whenOneRecipientDispatchFails() {
        Exchange exchange = standardExchange(ExchangeStatus.PENDING);
        doThrow(new RuntimeException("smtp is temporarily unavailable"))
                .doReturn(successVoid())
                .when(emailService)
                .sendNotificationEmail(any(NotificationEmailRequest.class));

        notificationDispatchService.sendExchangeCreatedNotifications(exchange);

        verify(emailService, times(2)).sendNotificationEmail(any(NotificationEmailRequest.class));
    }

    @Test
    void shouldDescribeGiftFlow_whenExchangeHasNoSenderBook() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Gift book", receiver);
        receiverBook.setIsGift(true);
        Exchange exchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                sender,
                receiver,
                null,
                receiverBook,
                ExchangeStatus.PENDING
        );
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        notificationDispatchService.sendExchangeCreatedNotifications(exchange);

        verify(emailService, times(2)).sendNotificationEmail(requestCaptor.capture());
        List<NotificationEmailRequest> requests = requestCaptor.getAllValues();
        assertThat(requests).anySatisfy(request -> {
            assertThat(request.getEmailTo()).isEqualTo("sender@example.com");
            assertThat(request.getExchange().getLeftBook().isPlaceholder()).isTrue();
            assertThat(request.getExchange().getRightBook().isGift()).isTrue();
        });
        assertThat(requests).anySatisfy(request -> {
            assertThat(request.getEmailTo()).isEqualTo("receiver@example.com");
            assertThat(request.getExchange().getLeftBook().isPlaceholder()).isTrue();
            assertThat(request.getExchange().getRightBook().isGift()).isTrue();
        });
        assertThat(requests).allSatisfy(request -> assertThat(request.getDetails()).isEmpty());
    }

    @Test
    void shouldSendNotificationForAdminBookUpdate() {
        User owner = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "owner_one");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Updated book", owner);
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);
        ArgumentCaptor<UserUpdate> updateCaptor = ArgumentCaptor.forClass(UserUpdate.class);

        notificationDispatchService.sendAdminBookUpdatedNotification(book, "admin@example.com");

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        verify(userUpdateRepository).save(updateCaptor.capture());
        NotificationEmailRequest request = requestCaptor.getValue();
        assertThat(request.getEmailTo()).isEqualTo("owner@example.com");
        assertThat(request.getTitle()).isEqualTo("Your book listing was updated by an admin");
        assertThat(request.getBook()).isNotNull();
        assertThat(request.getBook().getStatus().getValue()).isEqualTo("Active");
        assertThat(request.getHighlights()).isEmpty();
        assertThat(request.getDetails()).extracting(detail -> detail.getLabel())
                .containsExactly("Last updated");
        assertThat(updateCaptor.getValue().getType()).isEqualTo(UserUpdateType.ADMIN_BOOK_UPDATED);
        assertThat(updateCaptor.getValue().getBookName()).isEqualTo("Updated book");
        assertThat(updateCaptor.getValue().getTargetUrl()).isEqualTo("/app/my-books/" + book.getId());
    }

    @Test
    void shouldStoreActorUpdateWhenAdminUpdatesBook() {
        User owner = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "owner_one");
        User actor = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Updated book", owner);
        ArgumentCaptor<UserUpdate> updateCaptor = ArgumentCaptor.forClass(UserUpdate.class);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(actor));

        notificationDispatchService.sendAdminBookUpdatedNotification(book, "admin@example.com");

        verify(userUpdateRepository, times(2)).save(updateCaptor.capture());
        assertThat(updateCaptor.getAllValues()).extracting(UserUpdate::getType)
                .containsExactly(UserUpdateType.ADMIN_BOOK_UPDATED, UserUpdateType.ADMIN_BOOK_UPDATED_BY_YOU);
    }

    @Test
    void shouldOnlyNotifyOwnerWhenReportedBookIsUpdatedByAdmin() {
        User owner = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "owner_one");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Edited reported book", owner);
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        notificationDispatchService.sendAdminBookUpdatedNotification(book, "admin@example.com");

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        verify(userUpdateRepository).save(any(UserUpdate.class));
        assertThat(requestCaptor.getAllValues()).extracting(NotificationEmailRequest::getEmailTo)
                .containsExactly("owner@example.com");
    }

    @Test
    void shouldSendSnapshotBasedNotificationWhenAdminDeletesUser() {
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        notificationDispatchService.sendAdminUserDeletedNotification(
                UnitFixtureIds.TARGET_USER_ID,
                "target@example.com",
                "target_one",
                "en",
                "admin@example.com",
                Instant.parse("2026-04-23T10:15:30Z")
        );

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        NotificationEmailRequest request = requestCaptor.getValue();
        assertThat(request.getEmailTo()).isEqualTo("target@example.com");
        assertThat(request.getTitle()).isEqualTo("Your account was deleted");
        assertThat(request.getUser()).isNotNull();
        assertThat(request.getUser().getStatus().getValue()).isEqualTo("Deleted");
        assertThat(request.getHighlights()).isEmpty();
        assertThat(request.getDetails()).extracting(detail -> detail.getValue())
                .contains("23 Apr 2026, 12:15 Berlin time");
    }

    @Test
    void shouldStoreUserUpdateWhenAdminBansUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        ArgumentCaptor<UserUpdate> updateCaptor = ArgumentCaptor.forClass(UserUpdate.class);

        notificationDispatchService.sendAdminUserBannedNotification(user, "admin@example.com");

        verify(userUpdateRepository).save(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getType()).isEqualTo(UserUpdateType.ADMIN_USER_BANNED);
        assertThat(updateCaptor.getValue().getTargetUrl()).isEqualTo("/app/profile");
    }

    @Test
    void shouldStoreActorUpdateWhenAdminBansUser() {
        User actor = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        User targetUser = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        ArgumentCaptor<UserUpdate> updateCaptor = ArgumentCaptor.forClass(UserUpdate.class);

        notificationDispatchService.sendAdminUserBannedActorNotification(actor, targetUser);

        verify(userUpdateRepository).save(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getType()).isEqualTo(UserUpdateType.ADMIN_USER_BANNED_BY_YOU);
        assertThat(updateCaptor.getValue().getTargetUserId()).isEqualTo(targetUser.getId());
        assertThat(updateCaptor.getValue().getTargetUserNickname()).isEqualTo(targetUser.getNickname());
    }

    @Test
    void shouldStoreTargetAndActorUpdatesWhenAdminUnbansUser() {
        User actor = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        User targetUser = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        ArgumentCaptor<UserUpdate> updateCaptor = ArgumentCaptor.forClass(UserUpdate.class);

        notificationDispatchService.sendAdminUserUnbannedUpdates(actor, targetUser);

        verify(userUpdateRepository, times(2)).save(updateCaptor.capture());
        assertThat(updateCaptor.getAllValues()).extracting(UserUpdate::getType)
                .containsExactly(UserUpdateType.ADMIN_USER_UNBANNED, UserUpdateType.ADMIN_USER_UNBANNED_BY_YOU);
    }

    @Test
    void shouldStoreActorUpdateWhenAdminDeletesUser() {
        User actor = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        ArgumentCaptor<UserUpdate> updateCaptor = ArgumentCaptor.forClass(UserUpdate.class);

        notificationDispatchService.sendAdminUserDeletedActorNotification(
                actor,
                UnitFixtureIds.TARGET_USER_ID,
                "target_one",
                "target@example.com",
                null
        );

        verify(userUpdateRepository).save(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getType()).isEqualTo(UserUpdateType.ADMIN_USER_DELETED_BY_YOU);
        assertThat(updateCaptor.getValue().getTargetUserId()).isEqualTo(UnitFixtureIds.TARGET_USER_ID);
        assertThat(updateCaptor.getValue().getTargetUserNickname()).isEqualTo("target_one");
    }

    @Test
    void shouldSendReportResolutionNotification() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        User targetUser = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        Report report = UnitTestDataFactory.report(
                UnitFixtureIds.REPORT_ID,
                reporter,
                TargetType.USER,
                UnitFixtureIds.TARGET_USER_ID,
                ReportStatus.RESOLVED
        );
        report.captureUserSnapshot(targetUser);
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);
        mockDeletedFilterPassthrough();
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(targetUser));
        when(reportMapper.reportToSnapshotTargetUserDto(report)).thenReturn(ReportTargetUserDTO.builder()
                .id(targetUser.getId())
                .nickname(targetUser.getNickname())
                .build());

        notificationDispatchService.sendReportResolvedNotification(report, "admin@example.com");

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        NotificationEmailRequest request = requestCaptor.getValue();
        assertThat(request.getEmailTo()).isEqualTo("reporter@example.com");
        assertThat(request.getSubject()).isEqualTo("Your report was resolved");
        assertThat(request.getReport()).isNotNull();
        assertThat(request.getReport().getTargetUser()).isNotNull();
        assertThat(request.getReport().getTargetUser().getName()).isEqualTo("target_one");
        assertThat(request.getReport().getTargetStateText()).isNull();
        assertThat(request.getReport().getStatus().getValue()).isEqualTo("Processed");
        assertThat(request.getHighlights()).isEmpty();
        assertThat(request.getDetails()).extracting(detail -> detail.getValue())
                .allSatisfy(value -> assertThat(value).doesNotContain("4101", "Resolved the report", "1104"));
    }

    @Test
    void shouldStoreActorUpdateWhenAdminResolvesReport() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        User actor = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        User targetUser = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        Report report = UnitTestDataFactory.report(
                UnitFixtureIds.REPORT_ID,
                reporter,
                TargetType.USER,
                UnitFixtureIds.TARGET_USER_ID,
                ReportStatus.RESOLVED
        );
        report.captureUserSnapshot(targetUser);
        ArgumentCaptor<UserUpdate> updateCaptor = ArgumentCaptor.forClass(UserUpdate.class);

        mockDeletedFilterPassthrough();
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(targetUser));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(actor));
        when(reportMapper.reportToSnapshotTargetUserDto(report)).thenReturn(ReportTargetUserDTO.builder()
                .id(targetUser.getId())
                .nickname(targetUser.getNickname())
                .build());

        notificationDispatchService.sendReportResolvedNotification(report, "admin@example.com");

        verify(userUpdateRepository, times(2)).save(updateCaptor.capture());
        assertThat(updateCaptor.getAllValues()).extracting(UserUpdate::getType)
                .containsExactly(UserUpdateType.REPORT_RESOLVED, UserUpdateType.REPORT_RESOLVED_BY_YOU);
    }

    @Test
    void shouldNotifyReporterAndAdminsWhenReportIsSubmitted() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        User targetUser = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        User admin = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        admin.addRole(UserRole.ADMIN);
        User superAdmin = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "super@example.com", "super_one");
        superAdmin.addRole(UserRole.SUPER_ADMIN);
        Report report = UnitTestDataFactory.report(
                UnitFixtureIds.REPORT_ID,
                reporter,
                TargetType.USER,
                UnitFixtureIds.TARGET_USER_ID,
                ReportStatus.OPEN
        );
        report.captureUserSnapshot(targetUser);
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        mockDeletedFilterPassthrough();
        when(userRepository.findVerifiedUsersByAnyRole(Set.of(UserRole.ADMIN, UserRole.SUPER_ADMIN)))
                .thenReturn(List.of(admin, superAdmin));
        when(userRepository.findById(UnitFixtureIds.TARGET_USER_ID)).thenReturn(Optional.of(targetUser));
        when(reportMapper.reportToSnapshotTargetUserDto(report)).thenReturn(ReportTargetUserDTO.builder()
                .id(targetUser.getId())
                .nickname(targetUser.getNickname())
                .build());

        notificationDispatchService.sendReportSubmittedNotifications(report);

        verify(emailService, times(3)).sendNotificationEmail(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues()).extracting(NotificationEmailRequest::getEmailTo)
                .containsExactlyInAnyOrder("reporter@example.com", "admin@example.com", "super@example.com");
        assertThat(requestCaptor.getAllValues()).anySatisfy(request -> {
            assertThat(request.getEmailTo()).isEqualTo("reporter@example.com");
            assertThat(request.getSubject()).isEqualTo("Your report was submitted");
            assertThat(request.getReport().getStatus().getValue()).isEqualTo("Under review");
        });
        assertThat(requestCaptor.getAllValues()).anySatisfy(request -> {
            assertThat(request.getEmailTo()).isEqualTo("admin@example.com");
            assertThat(request.getSubject()).isEqualTo("New user report submitted");
            assertThat(request.getUser().getName()).isEqualTo("reporter_one");
        });
    }

    @Test
    void shouldSendBookTargetSnapshotWhenReportTargetsBook() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        User owner = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "owner_one");
        Book targetBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Charley Smash", owner);
        Report report = UnitTestDataFactory.report(
                UnitFixtureIds.REPORT_ID,
                reporter,
                TargetType.BOOK,
                UnitFixtureIds.SENDER_BOOK_ID,
                ReportStatus.RESOLVED
        );
        report.captureBookSnapshot(targetBook);
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        mockDeletedFilterPassthrough();
        when(bookRepository.findAllByIdIn(List.of(UnitFixtureIds.SENDER_BOOK_ID))).thenReturn(List.of(targetBook));
        when(reportMapper.reportToSnapshotTargetBookDto(report)).thenReturn(ReportTargetBookDTO.builder()
                .id(targetBook.getId())
                .name(targetBook.getName())
                .ownerUserId(owner.getId())
                .ownerNickname(owner.getNickname())
                .build());

        notificationDispatchService.sendReportResolvedNotification(report, "admin@example.com");

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        NotificationEmailRequest request = requestCaptor.getValue();
        assertThat(request.getReport()).isNotNull();
        assertThat(request.getReport().getTargetBook()).isNotNull();
        assertThat(request.getReport().getTargetBook().getName()).isEqualTo("Charley Smash");
        assertThat(request.getReport().getTargetBook().getMeta()).isEqualTo("Owner: owner_one");
        assertThat(request.getReport().getTargetBook().getSubtitle()).isNull();
        assertThat(request.getReport().getTargetUser()).isNull();
        assertThat(request.getReport().getTargetText()).isEqualTo("Book #2102");
        assertThat(request.getReport().getTargetStateText()).isNull();
    }

    @Test
    void shouldShowCurrentBookAndEditedNoticeWhenReportedBookWasEditedByModerator() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        User owner = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "owner_one");
        Book targetBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Edited title", owner);
        targetBook.setUpdatedBy(UnitFixtureIds.ADMIN_USER_ID);
        targetBook.setUpdatedAt(Instant.parse("2026-04-24T12:00:00Z"));
        Report report = UnitTestDataFactory.report(
                UnitFixtureIds.REPORT_ID,
                reporter,
                TargetType.BOOK,
                UnitFixtureIds.SENDER_BOOK_ID,
                ReportStatus.RESOLVED
        );
        report.setCreatedAt(Instant.parse("2026-04-24T10:00:00Z"));
        report.setTargetBookNameSnapshot("Original title");
        report.setTargetBookOwnerUserIdSnapshot(owner.getId());
        report.setTargetBookOwnerNicknameSnapshot(owner.getNickname());
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        mockDeletedFilterPassthrough();
        when(bookRepository.findAllByIdIn(List.of(UnitFixtureIds.SENDER_BOOK_ID))).thenReturn(List.of(targetBook));
        when(reportMapper.reportToSnapshotTargetBookDto(report)).thenReturn(ReportTargetBookDTO.builder()
                .id(targetBook.getId())
                .name("Original title")
                .ownerUserId(owner.getId())
                .ownerNickname(owner.getNickname())
                .build());

        notificationDispatchService.sendReportResolvedNotification(report, "admin@example.com");

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        NotificationEmailRequest request = requestCaptor.getValue();
        assertThat(request.getReport().getTargetBook().getName()).isEqualTo("Edited title");
        assertThat(request.getReport().getTargetModerationText()).isEqualTo(
                "The reported book was reviewed and edited by the moderation team. The card below shows the current listing."
        );
        assertThat(request.getReport().getTargetStateText()).isNull();
    }

    @Test
    void shouldPrioritizeOwnerDeletedNoticeWhenReportedBookOwnerWasDeleted() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        User owner = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "owner_one");
        owner.setDeletedAt(Instant.parse("2026-04-24T12:00:00Z"));
        Book targetBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Deleted owner book", owner);
        targetBook.setDeletedAt(Instant.parse("2026-04-24T12:00:00Z"));
        Report report = UnitTestDataFactory.report(
                UnitFixtureIds.REPORT_ID,
                reporter,
                TargetType.BOOK,
                UnitFixtureIds.SENDER_BOOK_ID,
                ReportStatus.RESOLVED
        );
        report.setTargetBookNameSnapshot(targetBook.getName());
        report.setTargetBookOwnerUserIdSnapshot(owner.getId());
        report.setTargetBookOwnerNicknameSnapshot(owner.getNickname());
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        mockDeletedFilterPassthrough();
        when(bookRepository.findAllByIdIn(List.of(UnitFixtureIds.SENDER_BOOK_ID))).thenReturn(List.of(targetBook));
        when(reportMapper.reportToSnapshotTargetBookDto(report)).thenReturn(ReportTargetBookDTO.builder()
                .id(targetBook.getId())
                .name(targetBook.getName())
                .ownerUserId(owner.getId())
                .ownerNickname(owner.getNickname())
                .build());

        notificationDispatchService.sendReportResolvedNotification(report, "admin@example.com");

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        NotificationEmailRequest request = requestCaptor.getValue();
        assertThat(request.getReport().getTargetBook()).isNull();
        assertThat(request.getReport().getTargetUser().getName()).isEqualTo("owner_one");
        assertThat(request.getReport().getTargetStateText()).isEqualTo(
                "The reported book owner was deleted together with their listed books."
        );
    }

    @Test
    void shouldFallbackToTargetTextWhenReportTargetSnapshotIsMissing() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        Report report = UnitTestDataFactory.report(
                UnitFixtureIds.REPORT_ID,
                reporter,
                TargetType.USER,
                UnitFixtureIds.TARGET_USER_ID,
                ReportStatus.REJECTED
        );
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        mockDeletedFilterPassthrough();
        when(userRepository.findById(UnitFixtureIds.TARGET_USER_ID)).thenReturn(Optional.empty());

        notificationDispatchService.sendReportRejectedNotification(report, "admin@example.com");

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        NotificationEmailRequest request = requestCaptor.getValue();
        assertThat(request.getReport()).isNotNull();
        assertThat(request.getReport().getTargetBook()).isNull();
        assertThat(request.getReport().getTargetUser()).isNull();
        assertThat(request.getReport().getTargetText()).isEqualTo("User #1104");
        assertThat(request.getReport().getTargetStateText()).isEqualTo(
                "The reported user has been deleted. The card below shows the historical snapshot captured when you created the report."
        );
        assertThat(request.getDetails()).extracting(detail -> detail.getValue())
                .allSatisfy(value -> assertThat(value).doesNotContain("Rejected the report", "1104"));
    }

    @Test
    void shouldUseHistoricalSnapshotAndDeletedNoticeWhenReportedUserWasDeleted() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        User deletedTargetUser = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        deletedTargetUser.setDeletedAt(Instant.parse("2026-04-24T08:15:30Z"));
        deletedTargetUser.setNickname("deleted_user_1104");
        deletedTargetUser.setPhotoUrl(null);
        Report report = UnitTestDataFactory.report(
                UnitFixtureIds.REPORT_ID,
                reporter,
                TargetType.USER,
                UnitFixtureIds.TARGET_USER_ID,
                ReportStatus.RESOLVED
        );
        report.setTargetUserNicknameSnapshot("snapshot_target_one");
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        mockDeletedFilterPassthrough();
        when(userRepository.findById(UnitFixtureIds.TARGET_USER_ID)).thenReturn(Optional.of(deletedTargetUser));
        when(reportMapper.reportToSnapshotTargetUserDto(report)).thenReturn(ReportTargetUserDTO.builder()
                .id(deletedTargetUser.getId())
                .nickname("snapshot_target_one")
                .build());

        notificationDispatchService.sendReportResolvedNotification(report, "admin@example.com");

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        NotificationEmailRequest request = requestCaptor.getValue();
        assertThat(request.getReport()).isNotNull();
        assertThat(request.getReport().getTargetUser()).isNotNull();
        assertThat(request.getReport().getTargetUser().getTitle()).isEqualTo("Reported user snapshot");
        assertThat(request.getReport().getTargetUser().getName()).isEqualTo("snapshot_target_one");
        assertThat(request.getReport().getTargetUser().getPhotoUrl()).isNull();
        assertThat(request.getReport().getTargetStateText()).isEqualTo(
                "The reported user has been deleted. The card below shows the historical snapshot captured when you created the report."
        );
    }

    @Test
    void shouldShowRestrictedUserNoticeWhenReportedUserIsCurrentlyBanned() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        User bannedTargetUser = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        bannedTargetUser.setBannedUntil(Instant.now().plusSeconds(3600));
        Report report = UnitTestDataFactory.report(
                UnitFixtureIds.REPORT_ID,
                reporter,
                TargetType.USER,
                UnitFixtureIds.TARGET_USER_ID,
                ReportStatus.RESOLVED
        );
        report.captureUserSnapshot(bannedTargetUser);
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        mockDeletedFilterPassthrough();
        when(userRepository.findById(UnitFixtureIds.TARGET_USER_ID)).thenReturn(Optional.of(bannedTargetUser));
        when(reportMapper.reportToSnapshotTargetUserDto(report)).thenReturn(ReportTargetUserDTO.builder()
                .id(bannedTargetUser.getId())
                .nickname(bannedTargetUser.getNickname())
                .build());

        notificationDispatchService.sendReportResolvedNotification(report, "admin@example.com");

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        NotificationEmailRequest request = requestCaptor.getValue();
        assertThat(request.getReport().getTargetStateText()).isNull();
        assertThat(request.getReport().getTargetModerationText()).isEqualTo(
                "The reported user account is currently restricted by the admin team."
        );
        assertThat(request.getDetails()).extracting(detail -> detail.getValue())
                .allSatisfy(value -> assertThat(value).doesNotContain("Resolved the report"));
    }

    @Test
    void shouldSendAdminAccessStatusWithoutRoleListWhenAdminRightsChange() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        notificationDispatchService.sendAdminRightsGrantedNotification(user, "admin@example.com");

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getUser()).isNotNull();
        assertThat(requestCaptor.getValue().getUser().getStatus().getValue()).isEqualTo("Admin");
        assertThat(requestCaptor.getValue().getDetails()).extracting(detail -> detail.getLabel())
                .containsExactly("Last updated");
    }

    @Test
    void shouldNotifySuperAdminActorWhenAdminRightsChange() {
        User actor = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "super@example.com", "super_one");
        User target = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        notificationDispatchService.sendAdminRightsActorNotification(actor, target, true);

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        NotificationEmailRequest request = requestCaptor.getValue();
        assertThat(request.getEmailTo()).isEqualTo("super@example.com");
        assertThat(request.getSubject()).isEqualTo("Admin rights were granted");
        assertThat(request.getUser().getName()).isEqualTo("target_one");
        assertThat(request.getUser().getStatus().getValue()).isEqualTo("Admin");
    }

    @Test
    void shouldNotifyUserWhenPasswordChanges() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        notificationDispatchService.sendPasswordChangedNotification(user);

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        NotificationEmailRequest request = requestCaptor.getValue();
        assertThat(request.getEmailTo()).isEqualTo("reader@example.com");
        assertThat(request.getSubject()).isEqualTo("Your password was changed");
        assertThat(request.getUser().getStatus().getValue()).isEqualTo("Changed");
    }

    @Test
    void shouldNotifyUserWhenOwnAccountIsDeleted() {
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        notificationDispatchService.sendUserSelfDeletedNotification(
                UnitFixtureIds.VERIFIED_USER_ID,
                "reader@example.com",
                "reader_one",
                "en",
                Instant.parse("2026-04-24T10:15:30Z")
        );

        verify(emailService).sendNotificationEmail(requestCaptor.capture());
        NotificationEmailRequest request = requestCaptor.getValue();
        assertThat(request.getEmailTo()).isEqualTo("reader@example.com");
        assertThat(request.getSubject()).isEqualTo("Your account was deleted");
        assertThat(request.getUser().getStatus().getValue()).isEqualTo("Deleted");
        assertThat(request.getEventTime()).isEqualTo("24 Apr 2026, 12:15 Berlin time");
    }

    @Test
    void shouldRegisterAfterCommitDispatchWhenTransactionSynchronizationIsActive() {
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
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        try {
            notificationDispatchService.sendExchangeCreatedNotifications(exchange);

            verifyNoInteractions(emailService);
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(emailService, times(2)).sendNotificationEmail(requestCaptor.capture());
            assertThat(requestCaptor.getAllValues()).extracting(NotificationEmailRequest::getSubject)
                    .containsExactlyInAnyOrder("Exchange request sent", "New exchange request received");
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void shouldLocalizeNotificationCopyForGermanRecipient() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        sender.setLocale("de");
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
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        notificationDispatchService.sendExchangeApprovedNotifications(exchange);

        verify(emailService, times(2)).sendNotificationEmail(requestCaptor.capture());
        NotificationEmailRequest germanRequest = requestCaptor.getAllValues().stream()
                .filter(request -> "sender@example.com".equals(request.getEmailTo()))
                .findFirst()
                .orElseThrow();

        assertThat(germanRequest.getSubject()).isEqualTo("Ihre Tauschanfrage wurde bestätigt");
        assertThat(germanRequest.getEyebrow()).isEqualTo("Tausch-Update");
        assertThat(germanRequest.getExchange().getReferenceText()).isNull();
        assertThat(germanRequest.getExchange().getStatus().getValue()).isEqualTo("Bestätigt");
        assertThat(germanRequest.getHighlights()).isEmpty();
        assertThat(germanRequest.getDetails()).isEmpty();
    }

    private Exchange standardExchange(ExchangeStatus status) {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);

        return UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                sender,
                receiver,
                senderBook,
                receiverBook,
                status
        );
    }

    private void assertExchangeNotifications(
            Runnable action,
            String expectedSenderSubject,
            String expectedReceiverSubject
    ) {
        ArgumentCaptor<NotificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(NotificationEmailRequest.class);

        action.run();

        verify(emailService, times(2)).sendNotificationEmail(requestCaptor.capture());
        List<NotificationEmailRequest> requests = requestCaptor.getAllValues();
        assertThat(requests).anySatisfy(request -> {
            assertThat(request.getEmailTo()).isEqualTo("sender@example.com");
            assertThat(request.getSubject()).isEqualTo(expectedSenderSubject);
            assertThat(request.getExchange()).isNotNull();
            assertThat(request.getExchange().getLeftUser().getName()).isEqualTo("sender_one");
            assertThat(request.getExchange().getRightUser().getName()).isEqualTo("receiver_one");
        });
        assertThat(requests).anySatisfy(request -> {
            assertThat(request.getEmailTo()).isEqualTo("receiver@example.com");
            assertThat(request.getSubject()).isEqualTo(expectedReceiverSubject);
            assertThat(request.getExchange()).isNotNull();
            assertThat(request.getExchange().getLeftUser().getName()).isEqualTo("sender_one");
            assertThat(request.getExchange().getRightUser().getName()).isEqualTo("receiver_one");
        });
    }

    private void mockDeletedFilterPassthrough() {
        when(softDeleteFilterHelper.runWithoutDeletedFilter(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<Object> supplier = invocation.getArgument(0);
            return supplier.get();
        });
    }

    private ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/messages_email");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    private AppProperties appProperties() {
        AppProperties appProperties = new AppProperties();
        appProperties.getNotification().setEmailBatchDelayMillis(0);
        return appProperties;
    }
}
