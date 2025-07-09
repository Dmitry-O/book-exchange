package com.example.bookexchange.mapper;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.models.Book;

public class BookMapper {

    public static BookDTO fromEntity(Book book) {
        BookDTO dto = new BookDTO();

        dto.setId(book.getId());
        dto.setName(book.getName());
        dto.setDescription(book.getDescription());
        dto.setAuthor(book.getAuthor());
        dto.setCategory(book.getCategory());
        dto.setPublicationYear(book.getPublicationYear());
        dto.setPhotoBase64(book.getPhotoBase64());
        dto.setCity(book.getCity());
        dto.setContactDetails(book.getContactDetails());
        dto.setIsGift(book.getIsGift());

        return dto;
    }

    public static Book toEntity(BookCreateDTO dto) {
        Book book = new Book();

        book.setName(dto.getName());
        book.setDescription(dto.getDescription());
        book.setAuthor(dto.getAuthor());
        book.setCategory(dto.getCategory());
        book.setPublicationYear(dto.getPublicationYear());
        book.setPhotoBase64(dto.getPhotoBase64());
        book.setCity(dto.getCity());
        book.setContactDetails(dto.getContactDetails());
        book.setIsGift(dto.getIsGift());

        return book;
    }
}
