package com.example.bookexchange.exchange.mapper;

import com.example.bookexchange.book.dto.BookCategoryDTO;
import com.example.bookexchange.book.dto.BookDTO;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.exchange.dto.ExchangeDTO;
import com.example.bookexchange.exchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.exchange.dto.ExchangeUnreadUpdateDTO;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class ExchangeMapper {

    @Mapping(target = "senderBookName", source = "senderBook.name")
    @Mapping(target = "senderBookPhotoUrl", source = "senderBook.photoUrl")
    @Mapping(target = "receiverBookName", source = "receiverBook.name")
    @Mapping(target = "receiverBookPhotoUrl", source = "receiverBook.photoUrl")
    @Mapping(target = "version", source = "version")
    public abstract ExchangeDTO exchangeToExchangeDto(Exchange exchange);

    @Mapping(target = "status", source = "exchange.status")
    @Mapping(target = "id", source = "exchange.id")
    @Mapping(target = "version", source = "exchange.version")
    @Mapping(target = "userNickname", source = "userNickname")
    @Mapping(target = "otherUserId", source = "otherUserId")
    @Mapping(target = "senderBook", source = "exchange.senderBook")
    @Mapping(target = "receiverBook", source = "exchange.receiverBook")
    public abstract ExchangeDetailsDTO exchangeToExchangeDetailsDto(
            Exchange exchange,
            Long otherUserId,
            String userNickname
    );

    @Mapping(target = "senderBookName", source = "exchange.senderBook.name")
    @Mapping(target = "senderBookPhotoUrl", source = "exchange.senderBook.photoUrl")
    @Mapping(target = "receiverBookName", source = "exchange.receiverBook.name")
    @Mapping(target = "receiverBookPhotoUrl", source = "exchange.receiverBook.photoUrl")
    @Mapping(target = "version", source = "exchange.version")
    public abstract ExchangeHistoryDTO exchangeToExchangeHistoryDto(Exchange exchange, UserExchangeRole userRole);

    @Mapping(target = "id", source = "exchange.id")
    @Mapping(target = "version", source = "exchange.version")
    @Mapping(target = "status", source = "exchange.status")
    @Mapping(target = "senderBook", source = "exchange.senderBook")
    @Mapping(target = "receiverBook", source = "exchange.receiverBook")
    @Mapping(target = "userNickname", source = "userNickname")
    @Mapping(target = "otherUserId", source = "otherUserId")
    @Mapping(target = "contactDetails", source = "contactDetails")
    @Mapping(target = "userExchangeRole", source = "userExchangeRole")
    public abstract ExchangeHistoryDetailsDTO exchangeToExchangeHistoryDetailsDto(
            Exchange exchange,
            Long otherUserId,
            String userNickname,
            String contactDetails,
            UserExchangeRole userExchangeRole
    );

    @Mapping(target = "id", source = "exchange.id")
    @Mapping(target = "version", source = "exchange.version")
    @Mapping(target = "status", source = "exchange.status")
    @Mapping(target = "userExchangeRole", source = "userExchangeRole")
    @Mapping(target = "updatedAt", source = "exchange.updatedAt")
    @Mapping(target = "otherBookId", expression = "java(resolveOtherBookId(exchange, userExchangeRole))")
    @Mapping(target = "otherBookName", expression = "java(resolveOtherBookName(exchange, userExchangeRole))")
    @Mapping(target = "otherUserNickname", expression = "java(resolveOtherUserNickname(exchange, userExchangeRole))")
    @Mapping(target = "otherUserId", expression = "java(resolveOtherUserId(exchange, userExchangeRole))")
    public abstract ExchangeUnreadUpdateDTO exchangeToExchangeUnreadUpdateDto(
            Exchange exchange,
            UserExchangeRole userExchangeRole
    );

    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "category", source = "category", qualifiedByName = "storageValueToBookCategory")
    @Mapping(target = "publicationYear", source = "publicationYear")
    @Mapping(target = "photoUrl", source = "photoUrl")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "contactDetails", source = "contactDetails")
    @Mapping(target = "isGift", source = "isGift")
    @Mapping(target = "isExchanged", source = "isExchanged")
    protected abstract BookDTO bookToBookDTO(Book book);

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

    protected Long resolveOtherBookId(Exchange exchange, UserExchangeRole userRole) {
        return userRole == UserExchangeRole.SENDER
                ? exchange.getReceiverBook().getId()
                : exchange.getSenderBook().getId();
    }

    protected String resolveOtherBookName(Exchange exchange, UserExchangeRole userRole) {
        return userRole == UserExchangeRole.SENDER
                ? exchange.getReceiverBook().getName()
                : exchange.getSenderBook().getName();
    }

    protected String resolveOtherUserNickname(Exchange exchange, UserExchangeRole userRole) {
        return userRole == UserExchangeRole.SENDER
                ? exchange.getReceiverUser().getNickname()
                : exchange.getSenderUser().getNickname();
    }

    protected Long resolveOtherUserId(Exchange exchange, UserExchangeRole userRole) {
        return userRole == UserExchangeRole.SENDER
                ? exchange.getReceiverUser().getId()
                : exchange.getSenderUser().getId();
    }

    @Named("storageValueToBookCategory")
    protected BookCategoryDTO mapBookCategory(String category) {
        return BookCategoryDTO.fromStorageValue(category);
    }
}
