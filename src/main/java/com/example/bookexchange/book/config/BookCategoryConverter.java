package com.example.bookexchange.book.config;

import com.example.bookexchange.book.dto.BookCategoryDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class BookCategoryConverter implements Converter<String, BookCategoryDTO> {

    @Override
    public BookCategoryDTO convert(String source) {
        return BookCategoryDTO.fromProperty(source);
    }
}
