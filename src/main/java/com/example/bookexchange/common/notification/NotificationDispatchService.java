package com.example.bookexchange.common.notification;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.user.model.User;

import java.time.Instant;
import java.util.List;

public interface NotificationDispatchService {

    void sendExchangeCreatedNotifications(Exchange exchange);

    void sendExchangeApprovedNotifications(Exchange exchange);

    void sendExchangeDeclinedBySenderNotifications(Exchange exchange);

    void sendExchangeDeclinedByReceiverNotifications(Exchange exchange);

    void sendExchangeAutoDeclinedNotifications(List<Exchange> exchanges);

    void sendAdminBookUpdatedNotification(Book book, String adminEmail);

    void sendAdminBookPhotoDeletedNotification(Book book, String adminEmail);

    void sendAdminBookDeletedNotification(Book book, String adminEmail);

    void sendAdminBookRestoredNotification(Book book, String adminEmail);

    void sendAdminRightsGrantedNotification(User user, String adminEmail);

    void sendAdminRightsRevokedNotification(User user, String adminEmail);

    void sendAdminRightsActorNotification(User actor, User targetUser, boolean granted);

    void sendAdminUserBannedNotification(User user, String adminEmail);

    void sendAdminUserBannedActorNotification(User actor, User targetUser);

    void sendAdminUserUnbannedUpdates(User actor, User targetUser);

    void sendAdminUserDeletedNotification(
            Long userId,
            String email,
            String nickname,
            String locale,
            String adminEmail,
            Instant deletedAt
    );

    void sendAdminUserDeletedActorNotification(
            User actor,
            Long targetUserId,
            String nickname,
            String email,
            String photoUrl
    );

    void sendReportResolvedNotification(Report report, String adminEmail);

    void sendReportRejectedNotification(Report report, String adminEmail);

    void sendReportSubmittedNotifications(Report report);

    void sendPasswordChangedNotification(User user);

    void sendUserSelfDeletedNotification(
            Long userId,
            String email,
            String nickname,
            String locale,
            Instant deletedAt
    );
}
