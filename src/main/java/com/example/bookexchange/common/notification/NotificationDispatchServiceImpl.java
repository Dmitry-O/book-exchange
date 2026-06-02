package com.example.bookexchange.common.notification;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.audit.service.SoftDeleteFilterHelper;
import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.email.EmailService;
import com.example.bookexchange.common.email.NotificationEmailBadge;
import com.example.bookexchange.common.email.NotificationEmailBook;
import com.example.bookexchange.common.email.NotificationEmailDetail;
import com.example.bookexchange.common.email.NotificationEmailExchange;
import com.example.bookexchange.common.email.NotificationEmailReport;
import com.example.bookexchange.common.email.NotificationEmailRequest;
import com.example.bookexchange.common.email.NotificationEmailUserCard;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import com.example.bookexchange.report.dto.ReportTargetBookDTO;
import com.example.bookexchange.report.dto.ReportTargetUserDTO;
import com.example.bookexchange.report.mapper.ReportMapper;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.report.model.ReportReason;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.report.repository.ReportRepository;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private static final String SUCCESS_TONE = "success";
    private static final String WARNING_TONE = "warning";
    private static final String DANGER_TONE = "danger";

    private final EmailService emailService;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final UserUpdateRepository userUpdateRepository;
    private final SoftDeleteFilterHelper softDeleteFilterHelper;
    private final ReportMapper reportMapper;
    private final NotificationDisplayLocalizer notificationDisplayLocalizer;
    private final MessageSource messageSource;
    private final AppProperties appProperties;
    private final TaskExecutor notificationTaskExecutor;

    @Override
    public void sendExchangeCreatedNotifications(Exchange exchange) {
        Instant happenedAt = Instant.now();

        dispatchAfterCommit(List.of(
                buildExchangeNotification(
                        exchange,
                        exchange.getSenderUser(),
                        UserExchangeRole.SENDER,
                        "email.notification.exchange.created.sender",
                        happenedAt
                ),
                buildExchangeNotification(
                        exchange,
                        exchange.getReceiverUser(),
                        UserExchangeRole.RECEIVER,
                        "email.notification.exchange.created.receiver",
                        happenedAt
                )
        ));
    }

    @Override
    public void sendExchangeApprovedNotifications(Exchange exchange) {
        Instant happenedAt = Instant.now();

        dispatchAfterCommit(List.of(
                buildExchangeNotification(
                        exchange,
                        exchange.getSenderUser(),
                        UserExchangeRole.SENDER,
                        "email.notification.exchange.approved.sender",
                        happenedAt
                ),
                buildExchangeNotification(
                        exchange,
                        exchange.getReceiverUser(),
                        UserExchangeRole.RECEIVER,
                        "email.notification.exchange.approved.receiver",
                        happenedAt
                )
        ));
    }

    @Override
    public void sendExchangeDeclinedBySenderNotifications(Exchange exchange) {
        Instant happenedAt = Instant.now();

        dispatchAfterCommit(List.of(
                buildExchangeNotification(
                        exchange,
                        exchange.getSenderUser(),
                        UserExchangeRole.SENDER,
                        "email.notification.exchange.declinedBySender.sender",
                        happenedAt
                ),
                buildExchangeNotification(
                        exchange,
                        exchange.getReceiverUser(),
                        UserExchangeRole.RECEIVER,
                        "email.notification.exchange.declinedBySender.receiver",
                        happenedAt
                )
        ));
    }

    @Override
    public void sendExchangeDeclinedByReceiverNotifications(Exchange exchange) {
        Instant happenedAt = Instant.now();

        dispatchAfterCommit(List.of(
                buildExchangeNotification(
                        exchange,
                        exchange.getSenderUser(),
                        UserExchangeRole.SENDER,
                        "email.notification.exchange.declinedByReceiver.sender",
                        happenedAt
                ),
                buildExchangeNotification(
                        exchange,
                        exchange.getReceiverUser(),
                        UserExchangeRole.RECEIVER,
                        "email.notification.exchange.declinedByReceiver.receiver",
                        happenedAt
                )
        ));
    }

    @Override
    public void sendExchangeAutoDeclinedNotifications(List<Exchange> exchanges) {
        Instant happenedAt = Instant.now();
        List<NotificationEmailRequest> requests = new ArrayList<>();

        for (Exchange exchange : exchanges) {
            if (!isDeleted(exchange.getSenderUser())) {
                requests.add(buildExchangeNotification(
                        exchange,
                        exchange.getSenderUser(),
                        UserExchangeRole.SENDER,
                        "email.notification.exchange.autoDeclined.sender",
                        happenedAt
                ));
            }

            if (!isDeleted(exchange.getReceiverUser())) {
                requests.add(buildExchangeNotification(
                        exchange,
                        exchange.getReceiverUser(),
                        UserExchangeRole.RECEIVER,
                        "email.notification.exchange.autoDeclined.receiver",
                        happenedAt
                ));
            }
        }

        dispatchAfterCommit(requests);
    }

    @Override
    public void sendAdminBookUpdatedNotification(Book book, String adminEmail) {
        Instant happenedAt = Instant.now();
        List<NotificationEmailRequest> requests = new ArrayList<>();

        saveBookUpdate(book.getUser(), UserUpdateType.ADMIN_BOOK_UPDATED, book, "/app/my-books/" + book.getId());
        findUserByEmail(adminEmail).ifPresent(actor ->
                saveBookUpdate(actor, UserUpdateType.ADMIN_BOOK_UPDATED_BY_YOU, book, "/admin/books/" + book.getId())
        );
        requests.add(buildBookNotification(
                book,
                "email.notification.admin.book.updated",
                adminEmail,
                happenedAt
        ));
        findUserByEmail(adminEmail).ifPresent(actor -> requests.add(
                buildBookActorNotification(actor, book, "email.notification.admin.book.updated.actor", happenedAt)
        ));

        dispatchAfterCommit(requests);
    }

    @Override
    public void sendAdminBookPhotoDeletedNotification(Book book, String adminEmail) {
        Instant happenedAt = Instant.now();
        List<NotificationEmailRequest> requests = new ArrayList<>();

        saveBookUpdate(book.getUser(), UserUpdateType.ADMIN_BOOK_PHOTO_DELETED, book, "/app/my-books/" + book.getId());
        findUserByEmail(adminEmail).ifPresent(actor ->
                saveBookUpdate(actor, UserUpdateType.ADMIN_BOOK_PHOTO_DELETED_BY_YOU, book, "/admin/books/" + book.getId())
        );
        requests.add(buildBookNotification(
                book,
                "email.notification.admin.book.photoDeleted",
                adminEmail,
                happenedAt
        ));
        findUserByEmail(adminEmail).ifPresent(actor -> requests.add(
                buildBookActorNotification(actor, book, "email.notification.admin.book.photoDeleted.actor", happenedAt)
        ));
        dispatchAfterCommit(requests);
    }

    @Override
    public void sendAdminBookDeletedNotification(Book book, String adminEmail) {
        Instant happenedAt = Instant.now();
        List<NotificationEmailRequest> requests = new ArrayList<>();

        saveBookUpdate(book.getUser(), UserUpdateType.ADMIN_BOOK_DELETED, book, "/app/my-books");
        findUserByEmail(adminEmail).ifPresent(actor ->
                saveBookUpdate(actor, UserUpdateType.ADMIN_BOOK_DELETED_BY_YOU, book, "/admin/books/" + book.getId())
        );
        requests.add(buildBookNotification(
                book,
                "email.notification.admin.book.deleted",
                adminEmail,
                happenedAt
        ));
        findUserByEmail(adminEmail).ifPresent(actor -> requests.add(
                buildBookActorNotification(actor, book, "email.notification.admin.book.deleted.actor", happenedAt)
        ));
        dispatchAfterCommit(requests);
    }

    @Override
    public void sendAdminBookRestoredNotification(Book book, String adminEmail) {
        Instant happenedAt = Instant.now();
        List<NotificationEmailRequest> requests = new ArrayList<>();

        saveBookUpdate(book.getUser(), UserUpdateType.ADMIN_BOOK_RESTORED, book, "/app/my-books/" + book.getId());
        findUserByEmail(adminEmail).ifPresent(actor ->
                saveBookUpdate(actor, UserUpdateType.ADMIN_BOOK_RESTORED_BY_YOU, book, "/admin/books/" + book.getId())
        );
        requests.add(buildBookNotification(
                book,
                "email.notification.admin.book.restored",
                adminEmail,
                happenedAt
        ));
        findUserByEmail(adminEmail).ifPresent(actor -> requests.add(
                buildBookActorNotification(actor, book, "email.notification.admin.book.restored.actor", happenedAt)
        ));
        dispatchAfterCommit(requests);
    }

    @Override
    public void sendAdminRightsGrantedNotification(User user, String adminEmail) {
        saveUserUpdate(user, UserUpdateType.ADMIN_RIGHTS_GRANTED, "/app/profile");
        dispatchAfterCommit(buildUserRoleNotification(
                user,
                "email.notification.admin.user.roleGranted",
                adminEmail,
                Instant.now(),
                true
        ));
    }

    @Override
    public void sendAdminRightsRevokedNotification(User user, String adminEmail) {
        saveUserUpdate(user, UserUpdateType.ADMIN_RIGHTS_REVOKED, "/app/profile");
        dispatchAfterCommit(buildUserRoleNotification(
                user,
                "email.notification.admin.user.roleRevoked",
                adminEmail,
                Instant.now(),
                false
        ));
    }

    @Override
    public void sendAdminRightsActorNotification(User actor, User targetUser, boolean granted) {
        saveUserUpdate(
                actor,
                granted ? UserUpdateType.ADMIN_RIGHTS_GRANTED_BY_YOU : UserUpdateType.ADMIN_RIGHTS_REVOKED_BY_YOU,
                targetUser,
                "/admin/users/" + targetUser.getId()
        );
        dispatchAfterCommit(buildAdminRightsActorNotification(actor, targetUser, Instant.now(), granted));
    }

    @Override
    public void sendAdminUserBannedNotification(User user, String adminEmail) {
        saveUserUpdate(user, UserUpdateType.ADMIN_USER_BANNED, "/app/profile");
        Locale locale = resolveLocale(user.getLocale());
        Instant happenedAt = Instant.now();
        String changedAt = formatInstant(happenedAt, locale);
        NotificationEmailBadge statusBadge = badge(label(locale, "status"), accountStatusText(locale, "BANNED"), DANGER_TONE);
        List<NotificationEmailRequest> requests = new ArrayList<>();

        requests.add(NotificationEmailRequest.builder()
                .emailTo(user.getEmail())
                .recipientName(user.getNickname())
                .locale(user.getLocale())
                .subject(message(locale, "email.notification.admin.user.banned.subject"))
                .preheader(message(locale, "email.notification.admin.user.banned.subject"))
                .eyebrow(message(locale, "email.notification.admin.eyebrow"))
                .title(message(locale, "email.notification.admin.user.banned.title"))
                .intro(message(locale, "email.notification.admin.user.banned.intro"))
                .summary(message(locale, "email.notification.admin.user.banned.summary"))
                .eventTime(changedAt)
                .user(buildUserCard(message(locale, "email.notification.admin.user.accountCardTitle"), user, statusBadge))
                .details(Stream.of(
                        detail(label(locale, "banType"), banTypeText(locale, user.isBannedPermanently())),
                        user.isBannedPermanently() ? null : detail(label(locale, "bannedUntil"), formatInstant(user.getBannedUntil(), locale)),
                        detail(label(locale, "banReason"), defaultText(user.getBanReason())),
                        detail(label(locale, "changedAt"), changedAt)
                ).filter(Objects::nonNull).toList())
                .build());
        findUserByEmail(adminEmail).ifPresent(actor -> requests.add(
                buildAdminUserActorNotification(actor, user, "email.notification.admin.user.banned.actor", happenedAt, DANGER_TONE)
        ));
        dispatchAfterCommit(requests);
    }

    @Override
    public void sendAdminUserBannedActorNotification(User actor, User targetUser) {
        saveUserUpdate(actor, UserUpdateType.ADMIN_USER_BANNED_BY_YOU, targetUser, "/admin/users/" + targetUser.getId());
    }

    @Override
    public void sendAdminUserUnbannedUpdates(User actor, User targetUser) {
        saveUserUpdate(targetUser, UserUpdateType.ADMIN_USER_UNBANNED, "/app/profile");
        saveUserUpdate(actor, UserUpdateType.ADMIN_USER_UNBANNED_BY_YOU, targetUser, "/admin/users/" + targetUser.getId());
        Instant happenedAt = Instant.now();
        dispatchAfterCommit(List.of(
                buildUserAccountNotification(
                        targetUser,
                        "email.notification.admin.user.unbanned",
                        happenedAt,
                        badge(label(resolveLocale(targetUser.getLocale()), "status"),
                                accountStatusText(resolveLocale(targetUser.getLocale()), "ACTIVE"),
                                SUCCESS_TONE)
                ),
                buildAdminUserActorNotification(actor, targetUser, "email.notification.admin.user.unbanned.actor", happenedAt, SUCCESS_TONE)
        ));
    }

    @Override
    public void sendAdminUserDeletedNotification(
            Long userId,
            String email,
            String nickname,
            String localeValue,
            String adminEmail,
            Instant deletedAt
    ) {
        Locale locale = resolveLocale(localeValue);
        String changedAt = formatInstant(deletedAt, locale);
        NotificationEmailBadge statusBadge = badge(label(locale, "status"), accountStatusText(locale, "DELETED"), DANGER_TONE);
        List<NotificationEmailRequest> requests = new ArrayList<>();

        requests.add(NotificationEmailRequest.builder()
                .emailTo(email)
                .recipientName(nickname)
                .locale(localeValue)
                .subject(message(locale, "email.notification.admin.user.deleted.subject"))
                .preheader(message(locale, "email.notification.admin.user.deleted.subject"))
                .eyebrow(message(locale, "email.notification.admin.eyebrow"))
                .title(message(locale, "email.notification.admin.user.deleted.title"))
                .intro(message(locale, "email.notification.admin.user.deleted.intro"))
                .summary(message(locale, "email.notification.admin.user.deleted.summary"))
                .eventTime(changedAt)
                .user(buildUserCard(message(locale, "email.notification.admin.user.accountCardTitle"), userId, nickname, email, statusBadge))
                .details(List.of(
                        detail(label(locale, "changedAt"), changedAt)
                ))
                .build());
        findUserByEmail(adminEmail).ifPresent(actor -> requests.add(
                buildAdminUserSnapshotActorNotification(
                        actor,
                        userId,
                        nickname,
                        email,
                        "email.notification.admin.user.deleted.actor",
                        deletedAt,
                        DANGER_TONE
                )
        ));
        dispatchAfterCommit(requests);
    }

    @Override
    public void sendAdminUserDeletedActorNotification(
            User actor,
            Long targetUserId,
            String nickname,
            String email,
            String photoUrl
    ) {
        if (isDeleted(actor)) {
            return;
        }

        UserUpdate update = baseUserUpdate(actor, UserUpdateType.ADMIN_USER_DELETED_BY_YOU, "/admin/users/" + targetUserId);
        update.setTargetUserId(targetUserId);
        update.setTargetUserNickname(nickname);
        update.setTargetUserPhotoUrl(photoUrl);
        userUpdateRepository.save(update);
    }

    @Override
    public void sendReportResolvedNotification(Report report, String adminEmail) {
        Instant happenedAt = Instant.now();
        List<NotificationEmailRequest> requests = new ArrayList<>();
        saveReportUpdate(report.getReporter(), UserUpdateType.REPORT_RESOLVED, report, "/app/my-reports");
        findUserByEmail(adminEmail).ifPresent(actor ->
                saveReportUpdate(actor, UserUpdateType.REPORT_RESOLVED_BY_YOU, report, "/admin/reports/" + report.getId())
        );
        requests.add(buildReportNotification(
                report,
                "email.notification.report.resolved",
                adminEmail,
                happenedAt
        ));
        findUserByEmail(adminEmail).ifPresent(actor -> requests.add(
                buildReportActorNotification(actor, report, "email.notification.report.resolved.actor", happenedAt)
        ));
        dispatchAfterCommit(requests);
    }

    @Override
    public void sendReportRejectedNotification(Report report, String adminEmail) {
        Instant happenedAt = Instant.now();
        List<NotificationEmailRequest> requests = new ArrayList<>();
        saveReportUpdate(report.getReporter(), UserUpdateType.REPORT_REJECTED, report, "/app/my-reports");
        findUserByEmail(adminEmail).ifPresent(actor ->
                saveReportUpdate(actor, UserUpdateType.REPORT_REJECTED_BY_YOU, report, "/admin/reports/" + report.getId())
        );
        requests.add(buildReportNotification(
                report,
                "email.notification.report.rejected",
                adminEmail,
                happenedAt
        ));
        findUserByEmail(adminEmail).ifPresent(actor -> requests.add(
                buildReportActorNotification(actor, report, "email.notification.report.rejected.actor", happenedAt)
        ));
        dispatchAfterCommit(requests);
    }

    @Override
    public void sendReportSubmittedNotifications(Report report) {
        Instant happenedAt = Instant.now();
        List<NotificationEmailRequest> requests = new ArrayList<>();

        saveReportUpdate(report.getReporter(), UserUpdateType.REPORT_SUBMITTED, report, "/app/my-reports");
        requests.add(buildReportNotification(
                report,
                "email.notification.report.submitted.reporter",
                null,
                happenedAt
        ));

        userRepository.findVerifiedUsersByAnyRole(Set.of(UserRole.ADMIN, UserRole.SUPER_ADMIN)).stream()
                .filter(admin -> admin.getDeletedAt() == null)
                .forEach(admin -> {
                    saveReportUpdate(admin, UserUpdateType.REPORT_SUBMITTED_ADMIN, report, "/admin/reports/" + report.getId());
                    requests.add(buildAdminReportSubmittedNotification(report, admin, happenedAt));
                });

        dispatchAfterCommit(requests);
    }

    @Override
    public void sendPasswordChangedNotification(User user) {
        saveUserUpdate(user, UserUpdateType.PASSWORD_CHANGED, "/app/profile");
        dispatchAfterCommit(buildUserAccountNotification(
                user,
                "email.notification.user.passwordChanged",
                Instant.now(),
                badge(label(resolveLocale(user.getLocale()), "status"),
                        accountStatusText(resolveLocale(user.getLocale()), "PASSWORD_CHANGED"),
                        SUCCESS_TONE)
        ));
    }

    @Override
    public void sendUserSelfDeletedNotification(
            Long userId,
            String email,
            String nickname,
            String localeValue,
            Instant deletedAt
    ) {
        Locale locale = resolveLocale(localeValue);
        NotificationEmailBadge statusBadge = badge(label(locale, "status"), accountStatusText(locale, "DELETED"), DANGER_TONE);

        dispatchAfterCommit(buildUserSnapshotNotification(
                userId,
                email,
                nickname,
                localeValue,
                "email.notification.user.selfDeleted",
                deletedAt,
                statusBadge
        ));
    }

    private NotificationEmailRequest buildExchangeNotification(
            Exchange exchange,
            User recipient,
            UserExchangeRole recipientRole,
            String messagePrefix,
            Instant happenedAt
    ) {
        Locale locale = resolveLocale(recipient.getLocale());
        String changedAt = formatInstant(happenedAt, locale);
        User senderUser = exchange.getSenderUser();
        User receiverUser = exchange.getReceiverUser();
        Book senderBook = exchange.getSenderBook();
        Book receiverBook = exchange.getReceiverBook();

        NotificationEmailBadge statusBadge = badge(
                label(locale, "status"),
                exchangeStatusText(locale, exchange.getStatus()),
                toneForExchangeStatus(exchange.getStatus())
        );

        NotificationEmailBook leftBook = senderBook != null
                ? buildBookCard(
                locale,
                message(
                        locale,
                        recipientRole == UserExchangeRole.SENDER
                                ? "email.notification.exchange.card.yourBook"
                                : "email.notification.exchange.card.offeredBook"
                ),
                senderBook
        )
                : buildGiftRequestCard(
                locale,
                "email.notification.exchange.card.giftRequest",
                recipientRole == UserExchangeRole.SENDER
                        ? "email.notification.exchange.gift.senderOwnSlot"
                        : "email.notification.exchange.gift.receiverOtherSlot"
        );

        NotificationEmailBook rightBook = receiverBook != null
                ? buildBookCard(
                locale,
                message(
                        locale,
                        recipientRole == UserExchangeRole.SENDER
                                ? "email.notification.exchange.card.requestedBook"
                                : "email.notification.exchange.card.yourBook"
                ),
                receiverBook
        )
                : buildGiftRequestCard(
                locale,
                "email.notification.exchange.card.giftRequest",
                recipientRole == UserExchangeRole.SENDER
                        ? "email.notification.exchange.gift.senderOtherSlot"
                        : "email.notification.exchange.gift.receiverOtherSlot"
        );

        NotificationEmailExchange exchangeSection = NotificationEmailExchange.builder()
                .exchangeId(String.valueOf(exchange.getId()))
                .status(statusBadge)
                .changedAt(changedAt)
                .leftBook(leftBook)
                .rightBook(rightBook)
                .leftUser(buildUserCard(
                        message(
                                locale,
                                recipientRole == UserExchangeRole.SENDER
                                        ? "email.notification.exchange.card.you"
                                        : "email.notification.exchange.card.requester"
                        ),
                        senderUser
                ))
                .rightUser(buildUserCard(
                        message(
                                locale,
                                recipientRole == UserExchangeRole.SENDER
                                        ? "email.notification.exchange.card.bookOwner"
                                        : "email.notification.exchange.card.you"
                        ),
                        receiverUser
                ))
                .build();

        return NotificationEmailRequest.builder()
                .emailTo(recipient.getEmail())
                .recipientName(recipient.getNickname())
                .locale(recipient.getLocale())
                .subject(message(locale, messagePrefix + ".subject"))
                .preheader(message(locale, messagePrefix + ".subject"))
                .eyebrow(message(locale, "email.notification.exchange.eyebrow"))
                .title(message(locale, messagePrefix + ".title"))
                .intro(message(locale, messagePrefix + ".intro"))
                .summary(message(locale, messagePrefix + ".summary"))
                .eventTime(changedAt)
                .ctaLabel(message(locale, "email.notification.cta.openExchange"))
                .ctaUrl(buildFrontendUrl(resolveExchangeCtaPath(exchange, recipientRole)))
                .exchange(exchangeSection)
                .build();
    }

    private NotificationEmailRequest buildBookNotification(
            Book book,
            String messagePrefix,
            String adminEmail,
            Instant happenedAt
    ) {
        User owner = book.getUser();
        Locale locale = resolveLocale(owner.getLocale());
        String changedAt = formatInstant(happenedAt, locale);
        String bookStatus = bookStatusText(locale, book.getDeletedAt() == null);
        NotificationEmailBadge statusBadge = badge(
                label(locale, "bookStatus"),
                bookStatus,
                book.getDeletedAt() == null ? SUCCESS_TONE : DANGER_TONE
        );

        return NotificationEmailRequest.builder()
                .emailTo(owner.getEmail())
                .recipientName(owner.getNickname())
                .locale(owner.getLocale())
                .subject(message(locale, messagePrefix + ".subject"))
                .preheader(message(locale, messagePrefix + ".subject"))
                .eyebrow(message(locale, "email.notification.admin.eyebrow"))
                .title(message(locale, messagePrefix + ".title"))
                .intro(message(locale, messagePrefix + ".intro"))
                .summary(message(locale, messagePrefix + ".summary"))
                .eventTime(changedAt)
                .ctaLabel(message(locale, book.getDeletedAt() == null ? "email.notification.cta.openBook" : "email.notification.cta.openBooks"))
                .ctaUrl(buildFrontendUrl(book.getDeletedAt() == null ? "/app/my-books/" + book.getId() : "/app/my-books"))
                .book(buildBookCard(locale, message(locale, "email.notification.admin.book.cardTitle"), book, statusBadge))
                .details(List.of(
                        detail(label(locale, "changedAt"), changedAt)
                ))
                .build();
    }

    private NotificationEmailRequest buildBookActorNotification(
            User actor,
            Book book,
            String messagePrefix,
            Instant happenedAt
    ) {
        Locale locale = resolveLocale(actor.getLocale());
        String changedAt = formatInstant(happenedAt, locale);
        NotificationEmailBadge statusBadge = badge(
                label(locale, "bookStatus"),
                bookStatusText(locale, book.getDeletedAt() == null),
                book.getDeletedAt() == null ? SUCCESS_TONE : DANGER_TONE
        );

        return NotificationEmailRequest.builder()
                .emailTo(actor.getEmail())
                .recipientName(actor.getNickname())
                .locale(actor.getLocale())
                .subject(message(locale, messagePrefix + ".subject"))
                .preheader(message(locale, messagePrefix + ".subject"))
                .eyebrow(message(locale, "email.notification.admin.eyebrow"))
                .title(message(locale, messagePrefix + ".title"))
                .intro(message(locale, messagePrefix + ".intro"))
                .summary(message(locale, messagePrefix + ".summary"))
                .eventTime(changedAt)
                .ctaLabel(message(locale, "email.notification.cta.openBook"))
                .ctaUrl(buildFrontendUrl("/admin/books/" + book.getId()))
                .book(buildBookCard(locale, message(locale, "email.notification.admin.book.targetCardTitle"), book, statusBadge))
                .details(List.of(detail(label(locale, "changedAt"), changedAt)))
                .build();
    }

    private List<NotificationEmailRequest> buildReportedBookUpdatedNotifications(
            Book book,
            String adminEmail,
            Instant happenedAt
    ) {
        List<Report> reports = reportRepository.findByTargetTypeAndTargetIdAndStatus(
                TargetType.BOOK,
                book.getId(),
                ReportStatus.OPEN
        );

        if (reports == null || reports.isEmpty()) {
            return List.of();
        }

        List<NotificationEmailRequest> requests = new ArrayList<>();

        for (Report report : reports) {
            if (report.getReporter() == null) {
                continue;
            }

            saveReportUpdate(report.getReporter(), UserUpdateType.REPORT_TARGET_BOOK_UPDATED, report, "/app/my-reports");
            requests.add(buildReportNotification(
                    report,
                    "email.notification.report.targetBookUpdated.reporter",
                    adminEmail,
                    happenedAt
            ));
        }

        return requests;
    }

    private NotificationEmailRequest buildUserRoleNotification(
            User user,
            String messagePrefix,
            String adminEmail,
            Instant happenedAt,
            boolean granted
    ) {
        Locale locale = resolveLocale(user.getLocale());
        String changedAt = formatInstant(happenedAt, locale);
        NotificationEmailBadge statusBadge = badge(
                label(locale, "adminAccess"),
                roleChangeText(locale, granted),
                granted ? SUCCESS_TONE : WARNING_TONE
        );

        return NotificationEmailRequest.builder()
                .emailTo(user.getEmail())
                .recipientName(user.getNickname())
                .locale(user.getLocale())
                .subject(message(locale, messagePrefix + ".subject"))
                .preheader(message(locale, messagePrefix + ".subject"))
                .eyebrow(message(locale, "email.notification.admin.eyebrow"))
                .title(message(locale, messagePrefix + ".title"))
                .intro(message(locale, messagePrefix + ".intro"))
                .summary(message(locale, messagePrefix + ".summary"))
                .eventTime(changedAt)
                .ctaLabel(message(locale, "email.notification.cta.openProfile"))
                .ctaUrl(buildFrontendUrl("/app/profile"))
                .user(buildUserCard(message(locale, "email.notification.admin.user.accountCardTitle"), user, statusBadge))
                .details(List.of(
                        detail(label(locale, "changedAt"), changedAt)
                ))
                .build();
    }

    private NotificationEmailRequest buildAdminRightsActorNotification(
            User actor,
            User targetUser,
            Instant happenedAt,
            boolean granted
    ) {
        Locale locale = resolveLocale(actor.getLocale());
        String changedAt = formatInstant(happenedAt, locale);
        String messagePrefix = granted
                ? "email.notification.admin.user.roleGranted.actor"
                : "email.notification.admin.user.roleRevoked.actor";
        NotificationEmailBadge statusBadge = badge(
                label(locale, "adminAccess"),
                roleChangeText(locale, granted),
                granted ? SUCCESS_TONE : WARNING_TONE
        );

        return NotificationEmailRequest.builder()
                .emailTo(actor.getEmail())
                .recipientName(actor.getNickname())
                .locale(actor.getLocale())
                .subject(message(locale, messagePrefix + ".subject"))
                .preheader(message(locale, messagePrefix + ".subject"))
                .eyebrow(message(locale, "email.notification.admin.eyebrow"))
                .title(message(locale, messagePrefix + ".title"))
                .intro(message(locale, messagePrefix + ".intro"))
                .summary(message(locale, messagePrefix + ".summary"))
                .eventTime(changedAt)
                .ctaLabel(message(locale, "email.notification.cta.openUser"))
                .ctaUrl(buildFrontendUrl("/admin/users/" + targetUser.getId()))
                .user(buildUserCard(message(locale, "email.notification.admin.user.targetAccountCardTitle"), targetUser, statusBadge))
                .details(List.of(detail(label(locale, "changedAt"), changedAt)))
                .build();
    }

    private NotificationEmailRequest buildAdminUserActorNotification(
            User actor,
            User targetUser,
            String messagePrefix,
            Instant happenedAt,
            String tone
    ) {
        Locale locale = resolveLocale(actor.getLocale());
        String changedAt = formatInstant(happenedAt, locale);
        NotificationEmailBadge statusBadge = badge(
                label(locale, "status"),
                resolveAdminTargetUserStatus(locale, targetUser),
                tone
        );

        return NotificationEmailRequest.builder()
                .emailTo(actor.getEmail())
                .recipientName(actor.getNickname())
                .locale(actor.getLocale())
                .subject(message(locale, messagePrefix + ".subject"))
                .preheader(message(locale, messagePrefix + ".subject"))
                .eyebrow(message(locale, "email.notification.admin.eyebrow"))
                .title(message(locale, messagePrefix + ".title"))
                .intro(message(locale, messagePrefix + ".intro"))
                .summary(message(locale, messagePrefix + ".summary"))
                .eventTime(changedAt)
                .ctaLabel(message(locale, "email.notification.cta.openUser"))
                .ctaUrl(buildFrontendUrl("/admin/users/" + targetUser.getId()))
                .user(buildUserCard(message(locale, "email.notification.admin.user.targetAccountCardTitle"), targetUser, statusBadge))
                .details(buildAdminUserActorDetails(locale, changedAt, targetUser))
                .build();
    }

    private NotificationEmailRequest buildAdminUserSnapshotActorNotification(
            User actor,
            Long targetUserId,
            String nickname,
            String email,
            String messagePrefix,
            Instant happenedAt,
            String tone
    ) {
        Locale locale = resolveLocale(actor.getLocale());
        String changedAt = formatInstant(happenedAt, locale);
        NotificationEmailBadge statusBadge = badge(
                label(locale, "status"),
                accountStatusText(locale, "DELETED"),
                tone
        );

        return NotificationEmailRequest.builder()
                .emailTo(actor.getEmail())
                .recipientName(actor.getNickname())
                .locale(actor.getLocale())
                .subject(message(locale, messagePrefix + ".subject"))
                .preheader(message(locale, messagePrefix + ".subject"))
                .eyebrow(message(locale, "email.notification.admin.eyebrow"))
                .title(message(locale, messagePrefix + ".title"))
                .intro(message(locale, messagePrefix + ".intro"))
                .summary(message(locale, messagePrefix + ".summary"))
                .eventTime(changedAt)
                .ctaLabel(message(locale, "email.notification.cta.openUser"))
                .ctaUrl(buildFrontendUrl("/admin/users/" + targetUserId))
                .user(buildUserCard(message(locale, "email.notification.admin.user.targetAccountCardTitle"), targetUserId, nickname, email, statusBadge))
                .details(List.of(detail(label(locale, "changedAt"), changedAt)))
                .build();
    }

    private NotificationEmailRequest buildUserAccountNotification(
            User user,
            String messagePrefix,
            Instant happenedAt,
            NotificationEmailBadge statusBadge
    ) {
        Locale locale = resolveLocale(user.getLocale());
        String changedAt = formatInstant(happenedAt, locale);

        return NotificationEmailRequest.builder()
                .emailTo(user.getEmail())
                .recipientName(user.getNickname())
                .locale(user.getLocale())
                .subject(message(locale, messagePrefix + ".subject"))
                .preheader(message(locale, messagePrefix + ".subject"))
                .eyebrow(message(locale, "email.notification.user.eyebrow"))
                .title(message(locale, messagePrefix + ".title"))
                .intro(message(locale, messagePrefix + ".intro"))
                .summary(message(locale, messagePrefix + ".summary"))
                .eventTime(changedAt)
                .ctaLabel(message(locale, "email.notification.cta.openProfile"))
                .ctaUrl(buildFrontendUrl("/app/profile"))
                .user(buildUserCard(message(locale, "email.notification.admin.user.accountCardTitle"), user, statusBadge))
                .details(List.of(detail(label(locale, "changedAt"), changedAt)))
                .build();
    }

    private NotificationEmailRequest buildUserSnapshotNotification(
            Long userId,
            String email,
            String nickname,
            String localeValue,
            String messagePrefix,
            Instant happenedAt,
            NotificationEmailBadge statusBadge
    ) {
        Locale locale = resolveLocale(localeValue);
        String changedAt = formatInstant(happenedAt, locale);

        return NotificationEmailRequest.builder()
                .emailTo(email)
                .recipientName(nickname)
                .locale(localeValue)
                .subject(message(locale, messagePrefix + ".subject"))
                .preheader(message(locale, messagePrefix + ".subject"))
                .eyebrow(message(locale, "email.notification.user.eyebrow"))
                .title(message(locale, messagePrefix + ".title"))
                .intro(message(locale, messagePrefix + ".intro"))
                .summary(message(locale, messagePrefix + ".summary"))
                .eventTime(changedAt)
                .user(buildUserCard(message(locale, "email.notification.admin.user.accountCardTitle"), userId, nickname, email, statusBadge))
                .details(List.of(detail(label(locale, "changedAt"), changedAt)))
                .build();
    }

    private NotificationEmailRequest buildReportNotification(
            Report report,
            String messagePrefix,
            String adminEmail,
            Instant happenedAt
    ) {
        User reporter = report.getReporter();
        Locale locale = resolveLocale(reporter.getLocale());
        String changedAt = formatInstant(happenedAt, locale);
        ResolvedReportTargetView targetView = resolveReportTargetView(report);
        String reportStatus = reportStatusText(locale, report.getStatus());

        return NotificationEmailRequest.builder()
                .emailTo(reporter.getEmail())
                .recipientName(reporter.getNickname())
                .locale(reporter.getLocale())
                .subject(message(locale, messagePrefix + ".subject"))
                .preheader(message(locale, messagePrefix + ".subject"))
                .eyebrow(message(locale, "email.notification.report.eyebrow"))
                .title(message(locale, messagePrefix + ".title"))
                .intro(message(locale, messagePrefix + ".intro"))
                .summary(message(locale, messagePrefix + ".summary"))
                .eventTime(changedAt)
                .ctaLabel(message(locale, "email.notification.cta.openMyReports"))
                .ctaUrl(buildFrontendUrl("/app/my-reports"))
                .report(NotificationEmailReport.builder()
                        .title(message(locale, "email.notification.report.cardTitle"))
                        .status(badge(label(locale, "reportStatus"), reportStatus, toneForReportStatus(report.getStatus())))
                        .targetStateText(targetStateText(locale, report, targetView))
                        .targetModerationText(targetModerationText(locale, report, targetView))
                        .targetText(message(locale, "email.notification.report.targetText", targetTypeText(locale, report.getTargetType()), String.valueOf(report.getTargetId())))
                        .targetBook(buildReportTargetBookCard(locale, targetView.targetBook(), targetView.deleted()))
                        .targetUser(buildReportTargetUserCard(locale, targetView.targetUser(), targetView.deleted() || targetView.ownerDeleted()))
                        .reason(reportReasonText(locale, report.getReason()))
                        .comment(defaultText(report.getComment()))
                        .build())
                .details(List.of(detail(label(locale, "changedAt"), changedAt)))
                .build();
    }

    private NotificationEmailRequest buildReportActorNotification(
            User actor,
            Report report,
            String messagePrefix,
            Instant happenedAt
    ) {
        Locale locale = resolveLocale(actor.getLocale());
        String changedAt = formatInstant(happenedAt, locale);
        ResolvedReportTargetView targetView = resolveReportTargetView(report);

        return NotificationEmailRequest.builder()
                .emailTo(actor.getEmail())
                .recipientName(actor.getNickname())
                .locale(actor.getLocale())
                .subject(message(locale, messagePrefix + ".subject"))
                .preheader(message(locale, messagePrefix + ".subject"))
                .eyebrow(message(locale, "email.notification.report.eyebrow"))
                .title(message(locale, messagePrefix + ".title"))
                .intro(message(locale, messagePrefix + ".intro"))
                .summary(message(locale, messagePrefix + ".summary"))
                .eventTime(changedAt)
                .ctaLabel(message(locale, "email.notification.cta.openReport"))
                .ctaUrl(buildFrontendUrl("/admin/reports/" + report.getId()))
                .user(buildUserCard(message(locale, "email.notification.report.reporterCardTitle"), report.getReporter()))
                .report(NotificationEmailReport.builder()
                        .title(message(locale, "email.notification.report.adminCardTitle"))
                        .status(badge(label(locale, "reportStatus"), reportStatusText(locale, report.getStatus()), toneForReportStatus(report.getStatus())))
                        .targetStateText(targetStateText(locale, report, targetView))
                        .targetModerationText(targetModerationText(locale, report, targetView))
                        .targetText(message(locale, "email.notification.report.targetText", targetTypeText(locale, report.getTargetType()), String.valueOf(report.getTargetId())))
                        .targetBook(buildReportTargetBookCard(locale, targetView.targetBook(), targetView.deleted()))
                        .targetUser(buildReportTargetUserCard(locale, targetView.targetUser(), targetView.deleted() || targetView.ownerDeleted()))
                        .reason(reportReasonText(locale, report.getReason()))
                        .comment(defaultText(report.getComment()))
                        .build())
                .details(List.of(detail(label(locale, "changedAt"), changedAt)))
                .build();
    }

    private NotificationEmailRequest buildAdminReportSubmittedNotification(
            Report report,
            User admin,
            Instant happenedAt
    ) {
        Locale locale = resolveLocale(admin.getLocale());
        String changedAt = formatInstant(happenedAt, locale);
        ResolvedReportTargetView targetView = resolveReportTargetView(report);

        return NotificationEmailRequest.builder()
                .emailTo(admin.getEmail())
                .recipientName(admin.getNickname())
                .locale(admin.getLocale())
                .subject(message(locale, "email.notification.report.submitted.admin.subject"))
                .preheader(message(locale, "email.notification.report.submitted.admin.subject"))
                .eyebrow(message(locale, "email.notification.report.eyebrow"))
                .title(message(locale, "email.notification.report.submitted.admin.title"))
                .intro(message(locale, "email.notification.report.submitted.admin.intro"))
                .summary(message(locale, "email.notification.report.submitted.admin.summary"))
                .eventTime(changedAt)
                .ctaLabel(message(locale, "email.notification.cta.openReport"))
                .ctaUrl(buildFrontendUrl("/admin/reports/" + report.getId()))
                .user(buildUserCard(message(locale, "email.notification.report.reporterCardTitle"), report.getReporter()))
                .report(NotificationEmailReport.builder()
                        .title(message(locale, "email.notification.report.adminCardTitle"))
                        .status(badge(label(locale, "reportStatus"), reportStatusText(locale, report.getStatus()), toneForReportStatus(report.getStatus())))
                        .targetStateText(targetStateText(locale, report, targetView))
                        .targetModerationText(targetModerationText(locale, report, targetView))
                        .targetText(message(locale, "email.notification.report.targetText", targetTypeText(locale, report.getTargetType()), String.valueOf(report.getTargetId())))
                        .targetBook(buildReportTargetBookCard(locale, targetView.targetBook(), targetView.deleted()))
                        .targetUser(buildReportTargetUserCard(locale, targetView.targetUser(), targetView.deleted() || targetView.ownerDeleted()))
                        .reason(reportReasonText(locale, report.getReason()))
                        .comment(defaultText(report.getComment()))
                        .build())
                .details(List.of(detail(label(locale, "changedAt"), changedAt)))
                .build();
    }

    private void saveBookUpdate(User recipient, UserUpdateType type, Book book, String targetUrl) {
        if (isDeleted(recipient)) {
            return;
        }

        UserUpdate update = baseUserUpdate(recipient, type, targetUrl);
        copyBookSnapshot(update, book);
        userUpdateRepository.save(update);
    }

    private void saveUserUpdate(User recipient, UserUpdateType type, String targetUrl) {
        saveUserUpdate(recipient, type, null, targetUrl);
    }

    private void saveUserUpdate(User recipient, UserUpdateType type, User targetUser, String targetUrl) {
        if (isDeleted(recipient)) {
            return;
        }

        UserUpdate update = baseUserUpdate(recipient, type, targetUrl);
        copyUserSnapshot(update, targetUser);
        userUpdateRepository.save(update);
    }

    private void saveReportUpdate(User recipient, UserUpdateType type, Report report, String targetUrl) {
        if (isDeleted(recipient)) {
            return;
        }

        UserUpdate update = baseUserUpdate(recipient, type, targetUrl);
        copyReportSnapshot(update, report);

        ResolvedReportTargetView targetView = resolveReportTargetView(report);

        if (targetView.targetBook() != null) {
            copyReportBookSnapshot(update, targetView.targetBook());
        }

        if (targetView.targetUser() != null) {
            copyReportUserSnapshot(update, targetView.targetUser());
        }

        userUpdateRepository.save(update);
    }

    private UserUpdate baseUserUpdate(User recipient, UserUpdateType type, String targetUrl) {
        UserUpdate update = new UserUpdate();
        update.setUser(recipient);
        update.setType(type);
        update.setIsRead(false);
        update.setTargetUrl(targetUrl);
        return update;
    }

    private void copyBookSnapshot(UserUpdate update, Book book) {
        if (book == null) {
            return;
        }

        update.setBookId(book.getId());
        update.setBookName(book.getName());
        update.setBookPhotoUrl(book.getPhotoUrl());
        update.setBookGift(book.getIsGift());
    }

    private void copyUserSnapshot(UserUpdate update, User targetUser) {
        if (targetUser == null) {
            return;
        }

        update.setTargetUserId(targetUser.getId());
        update.setTargetUserNickname(targetUser.getNickname());
        update.setTargetUserPhotoUrl(targetUser.getPhotoUrl());
    }

    private void copyReportSnapshot(UserUpdate update, Report report) {
        if (report == null) {
            return;
        }

        update.setReportId(report.getId());
        update.setReportTargetType(report.getTargetType());
        update.setReportReason(report.getReason());
        update.setReportStatus(report.getStatus());
    }

    private void copyReportBookSnapshot(UserUpdate update, ReportTargetBookDTO targetBook) {
        update.setBookId(targetBook.getId());
        update.setBookName(targetBook.getName());
        update.setBookPhotoUrl(targetBook.getPhotoUrl());
        update.setBookGift(false);
        update.setTargetUserId(targetBook.getOwnerUserId());
        update.setTargetUserNickname(targetBook.getOwnerNickname());
        update.setTargetUserPhotoUrl(targetBook.getOwnerPhotoUrl());
    }

    private void copyReportUserSnapshot(UserUpdate update, ReportTargetUserDTO targetUser) {
        update.setTargetUserId(targetUser.getId());
        update.setTargetUserNickname(targetUser.getNickname());
        update.setTargetUserPhotoUrl(targetUser.getPhotoUrl());
    }

    private Optional<User> findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(userRepository.findByEmail(email))
                .orElse(Optional.empty());
    }

    private void dispatchAfterCommit(NotificationEmailRequest request) {
        dispatchAfterCommit(List.of(request));
    }

    private void dispatchAfterCommit(List<NotificationEmailRequest> requests) {
        List<NotificationEmailRequest> preparedRequests = requests.stream()
                .filter(Objects::nonNull)
                .filter(request -> isDeliverableEmail(request.getEmailTo()))
                .toList();

        if (preparedRequests.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executeNotificationBatch(preparedRequests);
                }
            });
            return;
        }

        executeNotificationBatch(preparedRequests);
    }

    private void executeNotificationBatch(List<NotificationEmailRequest> requests) {
        try {
            notificationTaskExecutor.execute(() -> sendNotificationBatch(requests));
        } catch (RuntimeException ex) {
            log.warn(
                    "Failed to schedule notification email batch. Sending synchronously. batchSize={}, reason={}",
                    requests.size(),
                    ex.getMessage()
            );
            sendNotificationBatch(requests);
        }
    }

    private void sendNotificationBatch(List<NotificationEmailRequest> requests) {
        for (int index = 0; index < requests.size(); index++) {
            sendNotificationEmail(requests.get(index));

            if (index < requests.size() - 1 && !delayBeforeNextEmail(requests.size())) {
                return;
            }
        }
    }

    private boolean delayBeforeNextEmail(int batchSize) {
        long delayMillis = notificationEmailBatchDelayMillis();

        if (batchSize < 2 || delayMillis <= 0) {
            return true;
        }

        try {
            Thread.sleep(delayMillis);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Notification email batch delay was interrupted. delayMillis={}", delayMillis);
            return false;
        }
    }

    private long notificationEmailBatchDelayMillis() {
        if (appProperties.getNotification() == null) {
            return 0;
        }

        return Math.max(0, appProperties.getNotification().getEmailBatchDelayMillis());
    }

    private void sendNotificationEmail(NotificationEmailRequest request) {
        try {
            emailService.sendNotificationEmail(request);
        } catch (RuntimeException ex) {
            log.warn(
                    "Failed to dispatch notification email. emailTo={}, subject={}, exceptionType={}, reason={}",
                    request.getEmailTo(),
                    request.getSubject(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage()
            );
        }
    }

    private NotificationEmailDetail detail(String label, String value) {
        return new NotificationEmailDetail(label, defaultText(value));
    }

    private NotificationEmailBadge badge(String label, String value, String tone) {
        return NotificationEmailBadge.builder()
                .label(defaultText(label))
                .value(defaultText(value))
                .tone(defaultText(tone))
                .build();
    }

    private List<NotificationEmailDetail> buildAdminUserActorDetails(Locale locale, String changedAt, User targetUser) {
        return Stream.of(
                detail(label(locale, "changedAt"), changedAt),
                isCurrentlyRestricted(targetUser)
                        ? detail(label(locale, "banType"), banTypeText(locale, targetUser.isBannedPermanently()))
                        : null,
                isCurrentlyRestricted(targetUser) && !targetUser.isBannedPermanently()
                        ? detail(label(locale, "bannedUntil"), formatInstant(targetUser.getBannedUntil(), locale))
                        : null,
                isCurrentlyRestricted(targetUser) && !isBlank(targetUser.getBanReason())
                        ? detail(label(locale, "banReason"), targetUser.getBanReason())
                        : null
        ).filter(Objects::nonNull).toList();
    }

    private String resolveExchangeCtaPath(Exchange exchange, UserExchangeRole recipientRole) {
        if (exchange.getStatus() == ExchangeStatus.PENDING) {
            return recipientRole == UserExchangeRole.RECEIVER
                    ? "/app/exchanges/offers/" + exchange.getId()
                    : "/app/exchanges/requests/" + exchange.getId();
        }

        return "/app/history/" + exchange.getId();
    }

    private String buildFrontendUrl(String path) {
        String frontendUrl = defaultText(appProperties.getFrontendUrl());

        if (frontendUrl.endsWith("/") && path.startsWith("/")) {
            return frontendUrl.substring(0, frontendUrl.length() - 1) + path;
        }

        if (!frontendUrl.endsWith("/") && !path.startsWith("/")) {
            return frontendUrl + "/" + path;
        }

        return frontendUrl + path;
    }

    private NotificationEmailBook buildBookCard(Locale locale, String title, Book book) {
        return buildBookCard(locale, title, book, null);
    }

    private NotificationEmailBook buildBookCard(Locale locale, String title, Book book, NotificationEmailBadge status) {
        return NotificationEmailBook.builder()
                .title(title)
                .name(defaultText(book.getName()))
                .subtitle(joinNotBlank(", ", defaultText(book.getAuthor()), formatPublicationYear(book.getPublicationYear())))
                .meta(joinNotBlank(", ",
                        notificationDisplayLocalizer.localizeBookCategory(book.getCategory(), locale),
                        notificationDisplayLocalizer.localizeCity(book.getCity(), locale)))
                .photoUrl(book.getPhotoUrl())
                .status(status)
                .gift(Boolean.TRUE.equals(book.getIsGift()))
                .build();
    }

    private NotificationEmailBook buildGiftRequestCard(Locale locale, String titleKey, String textKey) {
        return NotificationEmailBook.builder()
                .title(message(locale, titleKey))
                .placeholder(true)
                .gift(true)
                .placeholderTitle(message(locale, titleKey))
                .placeholderText(message(locale, textKey))
                .build();
    }

    private NotificationEmailUserCard buildUserCard(String title, User user) {
        return buildUserCard(title, user, null);
    }

    private NotificationEmailUserCard buildUserCard(String title, User user, NotificationEmailBadge status) {
        return NotificationEmailUserCard.builder()
                .title(title)
                .name(defaultText(user != null ? user.getNickname() : null))
                .meta(defaultText(user != null ? user.getEmail() : null))
                .photoUrl(user != null ? user.getPhotoUrl() : null)
                .initial(resolveInitial(user != null ? user.getNickname() : null, user != null ? user.getEmail() : null))
                .status(status)
                .build();
    }

    private NotificationEmailUserCard buildUserCard(String title, Long userId, String nickname, String email) {
        return buildUserCard(title, userId, nickname, email, null);
    }

    private NotificationEmailUserCard buildUserCard(
            String title,
            Long userId,
            String nickname,
            String email,
            NotificationEmailBadge status
    ) {
        return NotificationEmailUserCard.builder()
                .title(title)
                .name(defaultText(nickname))
                .meta(defaultText(email))
                .photoUrl(null)
                .initial(resolveInitial(nickname, email))
                .status(status)
                .build();
    }

    private NotificationEmailBook buildReportTargetBookCard(Locale locale, ReportTargetBookDTO targetBook, boolean deleted) {
        if (targetBook == null) {
            return null;
        }

        return NotificationEmailBook.builder()
                .title(message(
                        locale,
                        deleted
                                ? "email.notification.report.targetBook.snapshotCardTitle"
                                : "email.notification.report.targetBook.cardTitle"
                ))
                .name(defaultText(targetBook.getName()))
                .meta(targetBook.getOwnerNickname() != null && !targetBook.getOwnerNickname().isBlank()
                        ? message(locale, "email.notification.report.targetBook.owner", targetBook.getOwnerNickname())
                        : "-")
                .photoUrl(targetBook.getPhotoUrl())
                .build();
    }

    private NotificationEmailUserCard buildReportTargetUserCard(Locale locale, ReportTargetUserDTO targetUser, boolean deleted) {
        if (targetUser == null) {
            return null;
        }

        return NotificationEmailUserCard.builder()
                .title(message(
                        locale,
                        deleted
                                ? "email.notification.report.targetUser.snapshotCardTitle"
                                : "email.notification.report.targetUser.cardTitle"
                ))
                .name(defaultText(targetUser.getNickname()))
                .photoUrl(targetUser.getPhotoUrl())
                .initial(resolveInitial(targetUser.getNickname(), String.valueOf(targetUser.getId())))
                .build();
    }

    private String formatInstant(Instant instant, Locale locale) {
        if (instant == null) {
            return "-";
        }

        ZoneId zoneId = ZoneId.of("Europe/Berlin");
        String formatted = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", locale)
                .withZone(zoneId)
                .format(instant);
        return formatted + switch (locale.getLanguage()) {
            case "ru" -> " по Берлину";
            case "de" -> " Berliner Zeit";
            default -> " Berlin time";
        };
    }

    private String formatPublicationYear(Integer publicationYear) {
        return publicationYear == null ? "-" : String.valueOf(publicationYear);
    }

    private ResolvedReportTargetView resolveReportTargetView(Report report) {
        if (report.getTargetType() == TargetType.USER) {
            User currentTargetUser = softDeleteFilterHelper.runWithoutDeletedFilter(() ->
                    userRepository.findById(report.getTargetId()).orElse(null)
            );

            boolean deleted = isDeleted(currentTargetUser);
            ReportTargetUserDTO targetUser = reportMapper.reportToSnapshotTargetUserDto(report);
            boolean restricted = !deleted && isCurrentlyRestricted(currentTargetUser);

            if (targetUser == null) {
                targetUser = ReportTargetUserDTO.builder().build();
            }

            targetUser.setId(report.getTargetId());

            if (isBlank(targetUser.getNickname()) && !deleted && currentTargetUser != null) {
                targetUser.setNickname(currentTargetUser.getNickname());
            }

            targetUser.setPhotoUrl(deleted || currentTargetUser == null ? null : currentTargetUser.getPhotoUrl());

            return new ResolvedReportTargetView(
                    isBlank(targetUser.getNickname()) ? null : targetUser,
                    null,
                    deleted,
                    restricted,
                    false,
                    false
            );
        }

        if (report.getTargetType() == TargetType.BOOK) {
            Book currentTargetBook = softDeleteFilterHelper.runWithoutDeletedFilter(() ->
                    bookRepository.findAllByIdIn(List.of(report.getTargetId())).stream()
                            .findFirst()
                            .orElse(null)
            );

            boolean deleted = isDeleted(currentTargetBook);
            boolean ownerDeleted = currentTargetBook != null
                    && currentTargetBook.getUser() != null
                    && isDeleted(currentTargetBook.getUser());
            ReportTargetBookDTO targetBook = reportMapper.reportToSnapshotTargetBookDto(report);

            if (targetBook == null) {
                targetBook = ReportTargetBookDTO.builder().build();
            }

            targetBook.setId(report.getTargetId());

            if (ownerDeleted) {
                ReportTargetUserDTO ownerTargetUser = ReportTargetUserDTO.builder()
                        .id(targetBook.getOwnerUserId() != null
                                ? targetBook.getOwnerUserId()
                                : currentTargetBook.getUser().getId())
                        .nickname(!isBlank(targetBook.getOwnerNickname())
                                ? targetBook.getOwnerNickname()
                                : currentTargetBook.getUser().getNickname())
                        .photoUrl(null)
                        .build();

                return new ResolvedReportTargetView(
                        isBlank(ownerTargetUser.getNickname()) ? null : ownerTargetUser,
                        null,
                        true,
                        false,
                        false,
                        true
                );
            }

            boolean updatedByModerator = isBookUpdatedByModerator(report, currentTargetBook);

            if (!deleted && currentTargetBook != null) {
                targetBook.setName(currentTargetBook.getName());

                if (currentTargetBook.getUser() != null) {
                    targetBook.setOwnerUserId(currentTargetBook.getUser().getId());
                    targetBook.setOwnerNickname(currentTargetBook.getUser().getNickname());
                }

                targetBook.setPhotoUrl(currentTargetBook.getPhotoUrl());
                targetBook.setOwnerPhotoUrl(currentTargetBook.getUser() != null && !isDeleted(currentTargetBook.getUser())
                        ? currentTargetBook.getUser().getPhotoUrl()
                        : null);
            } else {
                targetBook.setPhotoUrl(null);
                targetBook.setOwnerPhotoUrl(null);
            }

            return new ResolvedReportTargetView(
                    null,
                    isBlank(targetBook.getName()) ? null : targetBook,
                    deleted,
                    false,
                    updatedByModerator,
                    false
            );
        }

        return new ResolvedReportTargetView(null, null, false, false, false, false);
    }

    private Locale resolveLocale(String localeValue) {
        if (localeValue == null || localeValue.isBlank()) {
            return Locale.ENGLISH;
        }

        Locale locale = Locale.forLanguageTag(localeValue);

        return locale.getLanguage() == null || locale.getLanguage().isBlank()
                ? Locale.ENGLISH
                : locale;
    }

    private String message(Locale locale, String key, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }

    private String label(Locale locale, String suffix) {
        return message(locale, "email.notification.labels." + suffix);
    }

    private String enumValue(Locale locale, String prefix, Enum<?> value) {
        return message(locale, prefix + value.name());
    }

    private String exchangeStatusText(Locale locale, ExchangeStatus status) {
        return enumValue(locale, "email.notification.values.exchangeStatus.", status);
    }

    private String reportStatusText(Locale locale, ReportStatus status) {
        return enumValue(locale, "email.notification.values.reportStatus.", status);
    }

    private String targetTypeText(Locale locale, TargetType targetType) {
        return enumValue(locale, "email.notification.values.targetType.", targetType);
    }

    private String reportReasonText(Locale locale, ReportReason reportReason) {
        return enumValue(locale, "email.notification.values.reportReason.", reportReason);
    }

    private String reportTargetDeletedText(Locale locale, TargetType targetType) {
        return message(locale, "email.notification.report.targetDeleted." + targetType.name());
    }

    private String reportTargetModerationText(Locale locale, TargetType targetType) {
        return message(locale, "email.notification.report.targetModeration." + targetType.name());
    }

    private String targetStateText(Locale locale, Report report, ResolvedReportTargetView targetView) {
        if (targetView.ownerDeleted()) {
            return message(locale, "email.notification.report.targetOwnerDeleted.BOOK");
        }

        if (targetView.deleted()) {
            return reportTargetDeletedText(locale, report.getTargetType());
        }

        return null;
    }

    private String targetModerationText(Locale locale, Report report, ResolvedReportTargetView targetView) {
        if (targetView.deleted() || targetView.ownerDeleted()) {
            return null;
        }

        if (targetView.restricted()) {
            return reportTargetModerationText(locale, report.getTargetType());
        }

        if (targetView.updatedByModerator()) {
            return message(locale, "email.notification.report.targetBook.updatedByModerator");
        }

        return null;
    }

    private boolean isBookUpdatedByModerator(Report report, Book currentTargetBook) {
        if (currentTargetBook == null || currentTargetBook.getUser() == null) {
            return false;
        }

        Long updatedBy = currentTargetBook.getUpdatedBy();
        Long ownerId = currentTargetBook.getUser().getId();
        boolean updatedBySomeoneElse = updatedBy != null && !updatedBy.equals(ownerId);
        boolean nameChanged = !Objects.equals(currentTargetBook.getName(), report.getTargetBookNameSnapshot());

        if (!updatedBySomeoneElse && !nameChanged) {
            return false;
        }

        return report.getCreatedAt() == null
                || currentTargetBook.getUpdatedAt() == null
                || currentTargetBook.getUpdatedAt().isAfter(report.getCreatedAt());
    }

    private String bookStatusText(Locale locale, boolean active) {
        return message(locale, "email.notification.values.bookStatus." + (active ? "ACTIVE" : "DELETED"));
    }

    private String banTypeText(Locale locale, boolean permanent) {
        return message(locale, "email.notification.values.banType." + (permanent ? "PERMANENT" : "TEMPORARY"));
    }

    private String roleChangeText(Locale locale, boolean granted) {
        return message(locale, "email.notification.values.roleChange." + (granted ? "GRANTED" : "REVOKED"));
    }

    private String accountStatusText(Locale locale, String statusKey) {
        return message(locale, "email.notification.values.accountStatus." + statusKey);
    }

    private String resolveAdminTargetUserStatus(Locale locale, User user) {
        if (user == null) {
            return accountStatusText(locale, "ACTIVE");
        }

        if (user.getDeletedAt() != null) {
            return accountStatusText(locale, "DELETED");
        }

        if (user.isBannedPermanently()
                || (user.getBannedUntil() != null && user.getBannedUntil().isAfter(Instant.now()))) {
            return accountStatusText(locale, "BANNED");
        }

        return accountStatusText(locale, "ACTIVE");
    }

    private String resolveInitial(String preferredValue, String fallbackValue) {
        String source = preferredValue != null && !preferredValue.isBlank() ? preferredValue : defaultText(fallbackValue);

        return source == null || source.isBlank() || "-".equals(source)
                ? "B"
                : source.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String joinNotBlank(String delimiter, String... values) {
        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank() && !"-".equals(value))
                .reduce((left, right) -> left + delimiter + right)
                .orElse("-");
    }

    private String toneForExchangeStatus(ExchangeStatus status) {
        if (status == ExchangeStatus.APPROVED) {
            return SUCCESS_TONE;
        }

        if (status == ExchangeStatus.DECLINED) {
            return DANGER_TONE;
        }

        return WARNING_TONE;
    }

    private String toneForReportStatus(ReportStatus status) {
        if (status == ReportStatus.RESOLVED) {
            return SUCCESS_TONE;
        }

        if (status == ReportStatus.REJECTED) {
            return DANGER_TONE;
        }

        return WARNING_TONE;
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private boolean isDeliverableEmail(String value) {
        return value != null
                && !value.isBlank()
                && !value.toLowerCase(Locale.ROOT).endsWith("@anonymized.anonymized");
    }

    private boolean isDeleted(User user) {
        return user == null || user.getDeletedAt() != null;
    }

    private boolean isDeleted(Book book) {
        return book == null || book.getDeletedAt() != null;
    }

    private boolean isCurrentlyRestricted(User user) {
        return user != null
                && (user.isBannedPermanently()
                || (user.getBannedUntil() != null && user.getBannedUntil().isAfter(Instant.now())));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ResolvedReportTargetView(
            ReportTargetUserDTO targetUser,
            ReportTargetBookDTO targetBook,
            boolean deleted,
            boolean restricted,
            boolean updatedByModerator,
            boolean ownerDeleted
    ) {
    }
}
