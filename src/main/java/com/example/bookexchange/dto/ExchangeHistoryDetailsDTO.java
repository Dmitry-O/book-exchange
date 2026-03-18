package com.example.bookexchange.dto;

import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.UserExchangeRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ExchangeHistoryDetailsDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("userNickname")
    private String userNickname;

    @JsonProperty("status")
    private ExchangeStatus status;

    @JsonProperty("senderBook")
    private BookDTO senderBook;

    @JsonProperty("receiverBook")
    private BookDTO receiverBook;

    @JsonProperty("contactDetails")
    private String contactDetails;

    @JsonProperty("userExchangeRole")
    private UserExchangeRole userExchangeRole;
}
