package com.example.bookexchange.exchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ExchangeUpdateBookDTO {

    @Schema(example = "12")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "The Great Gatsby")
    @JsonProperty("name")
    private String name;

    @Schema(example = "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/books/12_1712582410000.jpg")
    @JsonProperty("photoUrl")
    private String photoUrl;

    @Schema(example = "false")
    @JsonProperty("isGift")
    private Boolean isGift;
}
