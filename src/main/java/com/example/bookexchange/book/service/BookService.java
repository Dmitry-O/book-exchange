package com.example.bookexchange.book.service;

import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.book.dto.BookCreateDTO;
import com.example.bookexchange.book.dto.BookDTO;
import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.user.model.User;
import org.springframework.data.domain.Page;

import java.time.Instant;

public interface BookService {

    Result<BookDTO> addUserBook(Long userId, BookCreateDTO dto);

    Result<Page<BookDTO>> findUserBooks(Long userId, PageQueryDTO queryDTO);

    Result<Page<BookDTO>> findExchangedUserBooks(Long userId, PageQueryDTO queryDTO);

    Result<Page<BookDTO>> findBooks(BookSearchDTO dto, PageQueryDTO queryDTO);

    Result<Void> deleteUserBookById(Long userId, Long bookId, Long version);

    Result<BookDTO> updateUserBookById(Long userId, Long bookId, BookUpdateDTO dto, Long version);

    Result<BookDTO> findUserBookById(Long userId, Long bookId);

    void softDeleteBooks(User user, Instant deletedAt);
}
