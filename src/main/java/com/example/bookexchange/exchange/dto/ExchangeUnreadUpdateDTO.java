package com.example.bookexchange.exchange.dto;

import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
public class ExchangeUnreadUpdateDTO {

    @Schema(example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "3")
    @JsonProperty("version")
    private Long version;

    @Schema(example = "DECLINED")
    @JsonProperty("status")
    private ExchangeStatus status;

    @Schema(example = "RECEIVER")
    @JsonProperty("userExchangeRole")
    private UserExchangeRole userExchangeRole;

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
    @JsonProperty("updatedAt")
    private Instant updatedAt;
}
