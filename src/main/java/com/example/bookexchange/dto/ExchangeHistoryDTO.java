package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExchangeHistoryDTO {

    @JsonProperty("id")
    @NotNull
    private Long id;

    @JsonProperty("senderBookPhotoBase64")
    private String senderBookPhotoBase64;

    @JsonProperty("receiverBookPhotoBase64")
    private String receiverBookPhotoBase64;

    @JsonProperty("senderBookName")
    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    private String senderBookName;

    @JsonProperty("receiverBookName")
    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    private String receiverBookName;

    @JsonProperty("isRead")
    private Boolean isRead;
}
