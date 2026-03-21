package com.example.bookexchange.services;

import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.dto.BookSearchDTO;
import com.example.bookexchange.dto.BookUpdateDTO;
import org.springframework.data.domain.Page;

public interface BookService {

    Result<BookDTO> addUserBook(Long userId, BookCreateDTO dto);

    Result<Page<BookDTO>> findUserBooks(Long userId, Integer pageIndex, Integer pageSize);

    Result<Page<BookDTO>> findExchangedUserBooks(Long userId,  Integer pageIndex, Integer pageSize);

    Result<Page<BookDTO>> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize);

    Result<Void> deleteUserBookById(Long userId, Long bookId, Long version);

    Result<BookDTO> updateUserBookById(Long userId, Long bookId, BookUpdateDTO dto, Long version);

    Result<BookDTO> findUserBookById(Long userId, Long bookId);
}
