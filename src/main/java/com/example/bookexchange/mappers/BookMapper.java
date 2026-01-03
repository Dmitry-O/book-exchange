package com.example.bookexchange.mappers;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.models.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BookMapper {

    @Mapping(target = "isGift", defaultValue = "false")
    @Mapping(target = "isExchanged", defaultValue = "false")
    Book bookDtoToBook(BookCreateDTO dto);

    BookDTO bookToBookDto(Book book);
}
