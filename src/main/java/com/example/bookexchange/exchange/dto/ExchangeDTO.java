package com.example.bookexchange.exchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ExchangeDTO {

    @Schema(example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "3")
    @JsonProperty("version")
    private Long version;

    @Schema(example = "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/books/15_1712582410000.jpg")
    @JsonProperty("senderBookPhotoUrl")
    private String senderBookPhotoUrl;

    @Schema(example = "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/84/books/19_1712582411000.jpg")
    @JsonProperty("receiverBookPhotoUrl")
    private String receiverBookPhotoUrl;

    @Schema(example = "Charley Smash")
    @JsonProperty("senderBookName")
    private String senderBookName;

    @Schema(example = "Peter Crunch")
    @JsonProperty("receiverBookName")
    private String receiverBookName;
}
