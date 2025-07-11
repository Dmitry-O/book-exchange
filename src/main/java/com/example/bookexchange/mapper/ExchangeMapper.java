package com.example.bookexchange.mapper;

import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.Exchange;

public class ExchangeMapper {

    public static ExchangeDTO fromEntity(Exchange exchange) {
        ExchangeDTO dto = new ExchangeDTO();
        Book senderBook = exchange.getSenderBook();
        Book receiverBook = exchange.getReceiverBook();

        dto.setId(exchange.getId());
        dto.setSenderBookName(senderBook.getName());
        dto.setReceiverBookName(receiverBook.getName());
        dto.setSenderBookPhotoBase64(senderBook.getPhotoBase64());
        dto.setReceiverBookPhotoBase64(receiverBook.getPhotoBase64());

        return dto;
    }

    public static ExchangeDetailsDTO fromEntityDetails(
            Exchange exchange,
            BookDTO senderBook,
            BookDTO receiverBook,
            String userNickname
    ) {
        ExchangeDetailsDTO dto = new ExchangeDetailsDTO();

        dto.setId(exchange.getId());
        dto.setExchangeStatus(exchange.getStatus());
        dto.setUserNickname(userNickname);
        dto.setSenderBook(senderBook);
        dto.setReceiverBook(receiverBook);

        return dto;
    }

    public static ExchangeHistoryDetailsDTO fromEntityHistoryDetails(
            Exchange exchange,
            BookDTO senderBook,
            BookDTO receiverBook,
            String userNickname,
            String contactDetails,
            String youAre
    ) {
        ExchangeHistoryDetailsDTO dto = new ExchangeHistoryDetailsDTO();

        dto.setId(exchange.getId());
        dto.setUserNickname(userNickname);
        dto.setSenderBook(senderBook);
        dto.setReceiverBook(receiverBook);
        dto.setContactDetails(contactDetails);
        dto.setYouAre(youAre);

        return dto;
    }
}
