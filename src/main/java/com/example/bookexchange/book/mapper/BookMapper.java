package com.example.bookexchange.book.mapper;

import com.example.bookexchange.book.dto.BookCreateDTO;
import com.example.bookexchange.book.dto.BookDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.model.Book;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    builder = @Builder(disableBuilder = true),
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BookMapper {

    @Mapping(target = "isGift", defaultValue = "false")
    @Mapping(target = "isExchanged", defaultValue = "false")
    Book bookDtoToBook(BookCreateDTO dto);

    BookDTO bookToBookDto(Book book);

    @Mapping(target = "name", source = "dto.name")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "author", source = "dto.author")
    @Mapping(target = "category", source = "dto.category")
    @Mapping(target = "publicationYear", source = "dto.publicationYear")
    @Mapping(target = "photoBase64", source = "dto.photoBase64")
    @Mapping(target = "city", source = "dto.city")
    @Mapping(target = "contactDetails", source = "dto.contactDetails")
    @Mapping(target = "isGift", source = "dto.isGift")
    void updateBookDtoToBook(BookUpdateDTO dto, @MappingTarget Book book);
}
