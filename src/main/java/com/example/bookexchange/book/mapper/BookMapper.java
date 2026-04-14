package com.example.bookexchange.book.mapper;

import com.example.bookexchange.book.dto.BookCreateDTO;
import com.example.bookexchange.book.dto.BookDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.model.Book;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    builder = @Builder(disableBuilder = true),
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = BookCategoryMapper.class
)
public interface BookMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "category", source = "category", qualifiedByName = "bookCategoryToStorageValue")
    @Mapping(target = "publicationYear", source = "publicationYear")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "contactDetails", source = "contactDetails")
    @Mapping(target = "isGift", source = "isGift", defaultValue = "false")
    @Mapping(target = "isExchanged", constant = "false")
    Book bookDtoToBook(BookCreateDTO dto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "ownerUserId", source = "user.id")
    @Mapping(target = "ownerNickname", source = "user.nickname")
    @Mapping(target = "ownerPhotoUrl", source = "user.photoUrl")
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
    BookDTO bookToBookDto(Book book);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "name", source = "dto.name")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "author", source = "dto.author")
    @Mapping(target = "category", source = "dto.category", qualifiedByName = "bookCategoryToStorageValue")
    @Mapping(target = "publicationYear", source = "dto.publicationYear")
    @Mapping(target = "city", source = "dto.city")
    @Mapping(target = "contactDetails", source = "dto.contactDetails")
    @Mapping(target = "isGift", source = "dto.isGift")
    void updateBookDtoToBook(BookUpdateDTO dto, @MappingTarget Book book);
}
