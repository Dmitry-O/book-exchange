package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ExchangeHistoryDTO {

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

    @JsonProperty("isRead")
    private Boolean isRead;
}
