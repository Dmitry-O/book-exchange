package com.example.bookexchange.common.notification;

import com.example.bookexchange.common.audit.model.AuditableEntity;
import com.example.bookexchange.report.model.ReportReason;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class UserUpdate extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserUpdateType type;

    @Column(nullable = false)
    private Boolean isRead = Boolean.FALSE;

    @Column(length = 255)
    private String targetUrl;

    private Long bookId;

    @Column(length = 25)
    private String bookName;

    @Column(length = 1024)
    private String bookPhotoUrl;

    private Boolean bookGift;

    private Long targetUserId;

    @Column(length = 20)
    private String targetUserNickname;

    @Column(length = 1024)
    private String targetUserPhotoUrl;

    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TargetType reportTargetType;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ReportReason reportReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReportStatus reportStatus;
}
