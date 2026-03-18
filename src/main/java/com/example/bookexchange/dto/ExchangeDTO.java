package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ExchangeDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("senderBookPhotoBase64")
    private String senderBookPhotoBase64;

    @JsonProperty("receiverBookPhotoBase64")
    private String receiverBookPhotoBase64;

    @JsonProperty("senderBookName")
    private String senderBookName;

    @JsonProperty("receiverBookName")
    private String receiverBookName;
}
