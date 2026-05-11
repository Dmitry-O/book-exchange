package com.example.bookexchange.exchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExchangeUpdateReadStateChangeDTO {

    @NotNull
    @Schema(example = "true", description = "Target read state for the current user")
    @JsonProperty("isRead")
    private Boolean isRead;
}
