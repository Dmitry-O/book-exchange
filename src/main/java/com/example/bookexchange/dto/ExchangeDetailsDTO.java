package com.example.bookexchange.dto;

import com.example.bookexchange.models.ExchangeStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExchangeDetailsDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("status")
    @NotNull
    private ExchangeStatus status;

    @JsonProperty("userNickname")
    private String userNickname;

    @JsonProperty("senderBook")
    private BookDTO senderBook;

    @JsonProperty("receiverBook")
    private BookDTO receiverBook;
}
