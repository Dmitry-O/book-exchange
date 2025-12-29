package com.example.bookexchange.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExchangeHistoryDTO {

    @NotNull
    private Long id;

    private String senderBookPhotoBase64;

    private String receiverBookPhotoBase64;

    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    private String senderBookName;

    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    private String receiverBookName;

    private Boolean isRead;
}
