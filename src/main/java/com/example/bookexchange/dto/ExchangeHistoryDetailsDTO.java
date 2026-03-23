package com.example.bookexchange.dto;

import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.UserExchangeRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ExchangeHistoryDetailsDTO {

    @Schema(example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "user_12345")
    @JsonProperty("userNickname")
    private String userNickname;

    @Schema(example = "APPROVED")
    @JsonProperty("status")
    private ExchangeStatus status;

    @JsonProperty("senderBook")
    private BookDTO senderBook;

    @JsonProperty("receiverBook")
    private BookDTO receiverBook;

    @Schema(example = "Juchostr. 12, 44320 Dortmund; 01512 0089 34 567")
    @JsonProperty("contactDetails")
    private String contactDetails;

    @Schema(example = "SENDER")
    @JsonProperty("userExchangeRole")
    private UserExchangeRole userExchangeRole;
}
