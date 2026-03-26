package com.example.bookexchange.exchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ExchangeDTO {

    @Schema(example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "Book photo")
    @JsonProperty("senderBookPhotoBase64")
    private String senderBookPhotoBase64;

    @Schema(example = "Book photo")
    @JsonProperty("receiverBookPhotoBase64")
    private String receiverBookPhotoBase64;

    @Schema(example = "Charley Smash")
    @JsonProperty("senderBookName")
    private String senderBookName;

    @Schema(example = "Peter Crunch")
    @JsonProperty("receiverBookName")
    private String receiverBookName;
}
