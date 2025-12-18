package com.example.bookexchange.mappers;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.UserExchangeRole;

import java.util.Objects;

public class ExchangeMapper {

    public static ExchangeDTO fromEntity(Exchange exchange) {
        ExchangeDTO dto = new ExchangeDTO();

        Book senderBook = exchange.getSenderBook();
        Book receiverBook = exchange.getReceiverBook();

        dto.setId(exchange.getId());
        dto.setReceiverBookName(receiverBook.getName());
        dto.setReceiverBookPhotoBase64(receiverBook.getPhotoBase64());

        if (senderBook != null) {
            dto.setSenderBookName(senderBook.getName());
            dto.setSenderBookPhotoBase64(senderBook.getPhotoBase64());
        }

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
        dto.setReceiverBook(receiverBook);

        if (senderBook != null) {
            dto.setSenderBook(senderBook);
        }

        return dto;
    }

    public static ExchangeHistoryDTO fromEntityHistory(Exchange exchange, UserExchangeRole userRole) {
        ExchangeHistoryDTO dto = new ExchangeHistoryDTO();

        Book senderBook = exchange.getSenderBook();
        Book receiverBook = exchange.getReceiverBook();

        dto.setId(exchange.getId());
        dto.setReceiverBookName(receiverBook.getName());
        dto.setReceiverBookPhotoBase64(receiverBook.getPhotoBase64());

        if (senderBook != null) {
            dto.setSenderBookName(senderBook.getName());
            dto.setSenderBookPhotoBase64(senderBook.getPhotoBase64());
        }

        if (Objects.equals(userRole, UserExchangeRole.SENDER)) {
            dto.setIsRead(exchange.getIsReadBySender());
        }

        if (Objects.equals(userRole, UserExchangeRole.RECEIVER)) {
            dto.setIsRead(exchange.getIsReadByReceiver());
        }

        return dto;
    }

    public static ExchangeHistoryDetailsDTO fromEntityHistoryDetails(
            Exchange exchange,
            BookDTO senderBook,
            BookDTO receiverBook,
            String userNickname,
            String contactDetails,
            UserExchangeRole userExchangeRole
    ) {
        ExchangeHistoryDetailsDTO dto = new ExchangeHistoryDetailsDTO();

        dto.setId(exchange.getId());
        dto.setUserNickname(userNickname);
        dto.setStatus(exchange.getStatus());
        dto.setSenderBook(senderBook);
        dto.setReceiverBook(receiverBook);
        dto.setContactDetails(contactDetails);
        dto.setUserExchangeRole(userExchangeRole);

        return dto;
    }
}
