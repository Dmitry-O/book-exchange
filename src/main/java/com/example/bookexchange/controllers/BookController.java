package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.services.BookService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class BookController {

    public static final String BOOK_PATH = "/api/v1/book";
    public static final String BOOK_PATH_USER = BOOK_PATH + "/user";
    public static final String BOOK_PATH_HISTORY = BOOK_PATH + "/history";
    public static final String BOOK_PATH_USER_BOOK_ID = BOOK_PATH_USER + "/{bookId}";
    public static final String BOOK_PATH_SEARCH = BOOK_PATH + "/search";

    private final BookService bookService;

    @PostMapping(BOOK_PATH_USER)
    public ResponseEntity<ApiMessage> addUserBook(@CurrentUser Long userId, @Validated @RequestBody BookCreateDTO dto) {
        return ResponseEntity.ok(new ApiMessage(bookService.addUserBook(userId, dto)));
    }

    @GetMapping(BOOK_PATH_USER)
    public Page<BookDTO> getUserBooks(
            @CurrentUser Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return bookService.findUserBooks(userId, pageIndex, pageSize);
    }

    @GetMapping(BOOK_PATH_HISTORY)
    public Page<BookDTO> getExchangedUserBooks(
            @CurrentUser Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return bookService.findExchangedUserBooks(userId, pageIndex, pageSize);
    }

    @GetMapping(BOOK_PATH_SEARCH)
    public Page<BookDTO> getBooks(
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @Validated @RequestBody(required = false) BookSearchDTO dto
    ) {
        return bookService.findBooks(dto, pageIndex, pageSize);
    }

    @DeleteMapping(BOOK_PATH_USER_BOOK_ID)
    public ResponseEntity<ApiMessage> deleteBookById(@CurrentUser Long userId, @PathVariable Long bookId) {
        return ResponseEntity.ok(new ApiMessage(bookService.deleteUserBookById(userId, bookId)));
    }

    @PatchMapping(BOOK_PATH_USER_BOOK_ID)
    public ResponseEntity<ApiMessage> updateUserBookById(@CurrentUser Long userId, @PathVariable Long bookId, @Validated @RequestBody BookUpdateDTO dto) {
        return ResponseEntity.ok(new ApiMessage(bookService.updateUserBookById(userId, bookId, dto)));
    }
}
