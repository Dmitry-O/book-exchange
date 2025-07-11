package com.example.bookexchange.dto;

import lombok.Data;

@Data
public class ExchangeHistoryDetailsDTO {
    private Long id;
    private String userNickname;
    private BookDTO senderBook;
    private BookDTO receiverBook;
    private String contactDetails;
    private String youAre;
}
