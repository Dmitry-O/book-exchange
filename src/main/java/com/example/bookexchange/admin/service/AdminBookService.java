package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.BookAdminDTO;
import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.common.result.Result;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetails;

public interface AdminBookService {

    Result<Page<BookAdminDTO>> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize, BookType bookType);

    Result<BookAdminDTO> findBookById(UserDetails adminUser, Long bookId);

    Result<BookAdminDTO> updateBookById(UserDetails adminUser, Long bookId, BookUpdateDTO dto, Long version);

    Result<BookAdminDTO> deleteBookById(UserDetails adminUser, Long bookId, Long version);

    Result<BookAdminDTO> restoreBookById(UserDetails adminUser, Long bookId, Long version);
}
