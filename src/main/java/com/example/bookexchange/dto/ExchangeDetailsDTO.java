package com.example.bookexchange.dto;

import com.example.bookexchange.models.ExchangeStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExchangeDetailsDTO {

    @JsonProperty("id")
    @NotNull
    private Long id;

    @JsonProperty("status")
    @NotNull
    private ExchangeStatus status;

    @JsonProperty("userNickname")
    @NotBlank
    @Size(min = 5, max = 20)
    private String userNickname;

    @JsonProperty("senderBook")
    private BookDTO senderBook;

    @JsonProperty("receiverBook")
    private BookDTO receiverBook;
}
