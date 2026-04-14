package com.example.bookexchange.book.mapper;

import com.example.bookexchange.book.dto.BookCategoryDTO;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class BookCategoryMapper {

    @Named("bookCategoryToStorageValue")
    public String toStorageValue(BookCategoryDTO category) {
        return category == null ? null : category.getProperty();
    }

    @Named("storageValueToBookCategory")
    public BookCategoryDTO toBookCategory(String category) {
        return BookCategoryDTO.fromStorageValue(category);
    }
}
