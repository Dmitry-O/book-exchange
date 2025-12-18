package com.example.bookexchange.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RequestCreateDTO {
    private Long senderUserId;
    private Long receiverUserId;
    private Long senderBookId;
    private Long receiverBookId;
}
