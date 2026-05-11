package com.example.bookexchange.report.model;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.audit.model.AuditableEntity;
import com.example.bookexchange.user.model.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Report extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    @NotNull
    private Long targetId;

    @Column(length = 20)
    private String targetUserNicknameSnapshot;

    @Column(length = 25)
    private String targetBookNameSnapshot;

    private Long targetBookOwnerUserIdSnapshot;

    @Column(length = 20)
    private String targetBookOwnerNicknameSnapshot;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    @NotBlank
    private String comment;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    private User reporter;

    public Report(
            Long id,
            TargetType targetType,
            Long targetId,
            ReportReason reason,
            String comment,
            ReportStatus status,
            User reporter
    ) {
        this.id = id;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.comment = comment;
        this.status = status;
        this.reporter = reporter;
    }

    public void captureUserSnapshot(User targetUser) {
        targetUserNicknameSnapshot = targetUser != null ? targetUser.getNickname() : null;
        targetBookNameSnapshot = null;
        targetBookOwnerUserIdSnapshot = null;
        targetBookOwnerNicknameSnapshot = null;
    }

    public void captureBookSnapshot(Book targetBook) {
        targetUserNicknameSnapshot = null;

        if (targetBook == null) {
            targetBookNameSnapshot = null;
            targetBookOwnerUserIdSnapshot = null;
            targetBookOwnerNicknameSnapshot = null;
            return;
        }

        targetBookNameSnapshot = targetBook.getName();
        targetBookOwnerUserIdSnapshot = targetBook.getUser() != null ? targetBook.getUser().getId() : null;
        targetBookOwnerNicknameSnapshot = targetBook.getUser() != null ? targetBook.getUser().getNickname() : null;
    }
}
