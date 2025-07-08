package com.example.bookexchange.services;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;

import java.util.List;

public interface BookService {
    BookDTO addUserBook(Long userId, BookCreateDTO dto);

    List<BookDTO> findUserBooks(Long userId);

    List<BookDTO> findBooks();

    String deleteUserBookById(Long userId, Long bookId);

    String updateUserBookById(Long userId, Long bookId, BookDTO dto);
}
