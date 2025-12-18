package com.example.bookexchange.mappers;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.models.Book;
import org.mapstruct.Mapper;

@Mapper
public interface BookMapper {

    Book bookDtoToBook(BookCreateDTO dto);
    BookDTO bookToBookDto(Book book);
}
