package com.example.bookexchange.dto;

import lombok.Data;

@Data
public class ExchangeDTO {
    private Long id;
    private String senderBookPhotoBase64;
    private String receiverBookPhotoBase64;
    private String senderBookName;
    private String receiverBookName;
}
