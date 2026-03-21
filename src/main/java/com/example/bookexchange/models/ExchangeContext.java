package com.example.bookexchange.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ExchangeContext {

    private Long senderUserId;
    private Long receiverUserId;
    private Long senderBookId;
    private Long receiverBookId;

    private Book senderBook;
    private Book receiverBook;

    private User senderUser;
    private User receiverUser;

    private Exchange exchange;
}
