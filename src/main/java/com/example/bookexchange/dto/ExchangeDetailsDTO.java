package com.example.bookexchange.dto;

import com.example.bookexchange.models.ExchangeStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExchangeDetailsDTO {

    @Schema(example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "PENDING")
    @JsonProperty("status")
    @NotNull
    private ExchangeStatus status;

    @Schema(example = "user_12345")
    @JsonProperty("userNickname")
    private String userNickname;

    @JsonProperty("senderBook")
    private BookDTO senderBook;

    @JsonProperty("receiverBook")
    private BookDTO receiverBook;
}
