package com.example.bookexchange.dto;

import com.example.bookexchange.models.ExchangeStatus;
import lombok.Data;

@Data
public class ExchangeDetailsDTO {
    private Long id;
    private ExchangeStatus exchangeStatus;
    private String userNickname;
    private BookDTO senderBook;
    private BookDTO receiverBook;
}
