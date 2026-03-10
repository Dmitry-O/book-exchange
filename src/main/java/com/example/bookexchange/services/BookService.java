package com.example.bookexchange.services;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.dto.BookSearchDTO;
import com.example.bookexchange.dto.BookUpdateDTO;
import org.springframework.data.domain.Page;

public interface BookService {

    String addUserBook(Long userId, BookCreateDTO dto);

    Page<BookDTO> findUserBooks(Long userId, Integer pageIndex, Integer pageSize);

    Page<BookDTO> findExchangedUserBooks(Long userId,  Integer pageIndex, Integer pageSize);

    Page<BookDTO> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize);

    String deleteUserBookById(Long userId, Long bookId);

    String updateUserBookById(Long userId, Long bookId, BookUpdateDTO dto);
}
