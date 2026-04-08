package com.example.bookexchange.exchange.dto;

import com.example.bookexchange.book.dto.BookDTO;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ExchangeHistoryDetailsDTO {

    @Schema(example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "3")
    @JsonProperty("version")
    private Long version;

    @Schema(example = "user_12345")
    @JsonProperty("userNickname")
    private String userNickname;

    @Schema(example = "42")
    @JsonProperty("otherUserId")
    private Long otherUserId;

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
