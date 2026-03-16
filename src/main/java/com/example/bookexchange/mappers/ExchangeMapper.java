package com.example.bookexchange.mappers;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.UserExchangeRole;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class ExchangeMapper {

    @Mapping(target = "senderBookName", source = "senderBook.name")
    @Mapping(target = "senderBookPhotoBase64", source = "senderBook.photoBase64")
    @Mapping(target = "receiverBookName", source = "receiverBook.name")
    @Mapping(target = "receiverBookPhotoBase64", source = "receiverBook.photoBase64")
    public abstract ExchangeDTO exchangeToExchangeDto(Exchange exchange);

    @Mapping(target = "status", source = "exchange.status")
    @Mapping(target = "id", source = "exchange.id")
    @Mapping(target = "userNickname", source = "userNickname")
    @Mapping(target = "senderBook", source = "exchange.senderBook")
    @Mapping(target = "receiverBook", source = "exchange.receiverBook")
    public abstract ExchangeDetailsDTO exchangeToExchangeDetailsDto(Exchange exchange, String userNickname);

    @Mapping(target = "senderBookName", source = "exchange.senderBook.name")
    @Mapping(target = "senderBookPhotoBase64", source = "exchange.senderBook.photoBase64")
    @Mapping(target = "receiverBookName", source = "exchange.receiverBook.name")
    @Mapping(target = "receiverBookPhotoBase64", source = "exchange.receiverBook.photoBase64")
    public abstract ExchangeHistoryDTO exchangeToExchangeHistoryDto(Exchange exchange, UserExchangeRole userRole);

    @Mapping(target = "id", source = "exchange.id")
    @Mapping(target = "status", source = "exchange.status")
    @Mapping(target = "senderBook", source = "exchange.senderBook")
    @Mapping(target = "receiverBook", source = "exchange.receiverBook")
    @Mapping(target = "userNickname", source = "userNickname")
    @Mapping(target = "contactDetails", source = "contactDetails")
    @Mapping(target = "userExchangeRole", source = "userExchangeRole")
    public abstract ExchangeHistoryDetailsDTO exchangeToExchangeHistoryDetailsDto(
            Exchange exchange,
            String userNickname,
            String contactDetails,
            UserExchangeRole userExchangeRole
    );

    @AfterMapping
    protected void setIsRead(
            @MappingTarget ExchangeHistoryDTO dto,
            Exchange exchange,
            UserExchangeRole userRole
    ) {

        if (userRole == UserExchangeRole.SENDER) {
            dto.setIsRead(exchange.getIsReadBySender());
        }

        if (userRole == UserExchangeRole.RECEIVER) {
            dto.setIsRead(exchange.getIsReadByReceiver());
        }
    }
}
