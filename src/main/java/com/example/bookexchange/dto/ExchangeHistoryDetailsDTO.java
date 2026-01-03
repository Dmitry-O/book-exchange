package com.example.bookexchange.dto;

import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.UserExchangeRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExchangeHistoryDetailsDTO {

    @JsonProperty("id")
    @NotNull
    private Long id;

    @JsonProperty("userNickname")
    @NotBlank
    @NotNull
    @Size(min = 5, max = 20)
    private String userNickname;

    @JsonProperty("status")
    @NotNull
    private ExchangeStatus status;

    @JsonProperty("senderBook")
    private BookDTO senderBook;

    @JsonProperty("receiverBook")
    private BookDTO receiverBook;

    @JsonProperty("contactDetails")
    @NotBlank
    @NotNull
    private String contactDetails;

    @JsonProperty("userExchangeRole")
    @NotBlank
    @NotNull
    private UserExchangeRole userExchangeRole;
}
