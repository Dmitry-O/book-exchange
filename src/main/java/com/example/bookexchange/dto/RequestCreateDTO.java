package com.example.bookexchange.dto;

import lombok.Data;

@Data
public class RequestCreateDTO {
    private Long senderUserId;
    private Long receiverUserId;
    private Long senderBookId;
    private Long receiverBookId;
}
