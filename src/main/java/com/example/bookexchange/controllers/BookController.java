package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.dto.BookSearchDTO;
import com.example.bookexchange.services.BookService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class BookController {

    public static final String BOOK_PATH ="/api/v1/book";
    public static final String USER_ID_PATH = "/{userId}";
    public static final String BOOK_PATH_USER_ID = BOOK_PATH + USER_ID_PATH;
    public static final String BOOK_PATH_HISTORY_USER_ID = BOOK_PATH + "/history" + USER_ID_PATH;
    public static final String BOOK_PATH_USER_ID_BOOK_ID = BOOK_PATH_USER_ID + "/{bookId}";

    private final BookService bookService;

    @PostMapping(BOOK_PATH_USER_ID)
    public BookDTO addUserBook(@PathVariable("userId") Long userId, @RequestBody BookCreateDTO dto) {
        return bookService.addUserBook(userId, dto);
    }

    @GetMapping(BOOK_PATH_USER_ID)
    public Page<BookDTO> getUserBooks(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return bookService.findUserBooks(userId, pageIndex, pageSize);
    }

    @GetMapping(BOOK_PATH_HISTORY_USER_ID)
    public Page<BookDTO> getExchangedUserBooks(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return bookService.findExchangedUserBooks(userId, pageIndex, pageSize);
    }

    @GetMapping(BOOK_PATH)
    public Page<BookDTO> getBooks(
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestBody(required = false) BookSearchDTO dto
    ) {
        return bookService.findBooks(dto, pageIndex, pageSize);
    }

    @DeleteMapping(BOOK_PATH_USER_ID_BOOK_ID)
    public String deleteBookById(@PathVariable("userId") Long userId, @PathVariable("bookId") Long bookId) {
        return bookService.deleteUserBookById(userId, bookId);
    }

    @PatchMapping(BOOK_PATH_USER_ID_BOOK_ID)
    public String updateUserBookById(@PathVariable("userId") Long userId, @PathVariable("bookId") Long bookId, @RequestBody BookDTO dto) {
        return bookService.updateUserBookById(userId, bookId, dto);
    }
}
