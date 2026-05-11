package com.example.bookexchange.exchange.dto;

import com.example.bookexchange.common.notification.UserUpdateType;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import com.example.bookexchange.report.model.ReportReason;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
public class ExchangeUpdateDTO {

    @Schema(example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "EXCHANGE")
    @JsonProperty("updateType")
    private UserUpdateType updateType;

    @Schema(example = "15")
    @JsonProperty("notificationId")
    private Long notificationId;

    @Schema(example = "1")
    @JsonProperty("exchangeId")
    private Long exchangeId;

    @Schema(example = "3")
    @JsonProperty("version")
    private Long version;

    @Schema(example = "DECLINED")
    @JsonProperty("status")
    private ExchangeStatus status;

    @Schema(example = "RECEIVER")
    @JsonProperty("userExchangeRole")
    private UserExchangeRole userExchangeRole;

    @Schema(example = "false")
    @JsonProperty("isRead")
    private Boolean isRead;

    @Schema(example = "false")
    @JsonProperty("autoDeclined")
    private Boolean autoDeclined;

    @Schema(example = "/app/my-books/12")
    @JsonProperty("targetUrl")
    private String targetUrl;

    @JsonProperty("senderBook")
    private ExchangeUpdateBookDTO senderBook;

    @JsonProperty("receiverBook")
    private ExchangeUpdateBookDTO receiverBook;

    @JsonProperty("book")
    private ExchangeUpdateBookDTO book;

    @Schema(example = "42")
    @JsonProperty("targetUserId")
    private Long targetUserId;

    @Schema(example = "user_12345")
    @JsonProperty("targetUserNickname")
    private String targetUserNickname;

    @Schema(example = "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/profile.jpg")
    @JsonProperty("targetUserPhotoUrl")
    private String targetUserPhotoUrl;

    @Schema(example = "5")
    @JsonProperty("reportId")
    private Long reportId;

    @Schema(example = "BOOK")
    @JsonProperty("reportTargetType")
    private TargetType reportTargetType;

    @Schema(example = "SPAM")
    @JsonProperty("reportReason")
    private ReportReason reportReason;

    @Schema(example = "RESOLVED")
    @JsonProperty("reportStatus")
    private ReportStatus reportStatus;

    @Schema(example = "2")
    @JsonProperty("otherBookId")
    private Long otherBookId;

    @Schema(example = "Peter Crunch")
    @JsonProperty("otherBookName")
    private String otherBookName;

    @Schema(example = "user_12345")
    @JsonProperty("otherUserNickname")
    private String otherUserNickname;

    @Schema(example = "42")
    @JsonProperty("otherUserId")
    private Long otherUserId;

    @Schema(example = "2026-04-06T12:15:00Z")
    @JsonProperty("updateCreatedAt")
    private Instant updateCreatedAt;

    @Schema(example = "42")
    @JsonProperty("declinerUserId")
    private Long declinerUserId;

    @Schema(example = "user_12345")
    @JsonProperty("declinerUserNickname")
    private String declinerUserNickname;

    @Schema(example = "SENDER")
    @JsonProperty("declinerUserRole")
    private UserExchangeRole declinerUserRole;
}
