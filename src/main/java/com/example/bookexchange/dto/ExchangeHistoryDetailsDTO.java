package com.example.bookexchange.dto;

import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.UserExchangeRole;
import lombok.Data;

@Data
public class ExchangeHistoryDetailsDTO {
    private Long id;
    private String userNickname;
    private ExchangeStatus status;
    private BookDTO senderBook;
    private BookDTO receiverBook;
    private String contactDetails;
    private UserExchangeRole userExchangeRole;
}
