package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.core.web.ResultResponseMapper;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.services.BookService;
import com.example.bookexchange.util.ParserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class BookController {

    private final ParserUtil parserUtil;
    private final BookService bookService;
    private final ResultResponseMapper responseMapper;

    public static final String BOOK_PATH = "/api/v1/book";
    public static final String BOOK_PATH_USER = BOOK_PATH + "/user";
    public static final String BOOK_PATH_HISTORY = BOOK_PATH + "/history";
    public static final String BOOK_PATH_USER_BOOK_ID = BOOK_PATH_USER + "/{bookId}";
    public static final String BOOK_PATH_SEARCH = BOOK_PATH + "/search";

    @PostMapping(BOOK_PATH_USER)
    public ResponseEntity<?> addUserBook(
            @CurrentUser Long userId,
            @Validated @RequestBody BookCreateDTO dto,
            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.addUserBook(userId, dto), request);
    }

    @GetMapping(BOOK_PATH_USER)
    public ResponseEntity<?> getUserBooks(
            @CurrentUser Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.findUserBooks(userId, pageIndex, pageSize), request);
    }

    @GetMapping(BOOK_PATH_USER_BOOK_ID)
    public ResponseEntity<?> getUserBookById(
            @CurrentUser Long userId,
            @PathVariable Long bookId,
            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.findUserBookById(userId, bookId), request);
    }

    @GetMapping(BOOK_PATH_HISTORY)
    public ResponseEntity<?> getExchangedUserBooks(
            @CurrentUser Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.findExchangedUserBooks(userId, pageIndex, pageSize), request);
    }

    @GetMapping(BOOK_PATH_SEARCH)
    public ResponseEntity<?> getBooks(
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @Validated @RequestBody(required = false) BookSearchDTO dto,
            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.findBooks(dto, pageIndex, pageSize), request);
    }

    @DeleteMapping(BOOK_PATH_USER_BOOK_ID)
    public ResponseEntity<?> deleteBookById(
            @CurrentUser Long userId,
            @PathVariable Long bookId,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.deleteUserBookById(userId, bookId, parserUtil.ifMatchParser(ifMatch)), request);
    }

    @PatchMapping(BOOK_PATH_USER_BOOK_ID)
    public ResponseEntity<?> updateUserBookById(
            @CurrentUser Long userId,
            @PathVariable Long bookId,
            @RequestHeader("If-Match") String ifMatch,
            @Validated @RequestBody BookUpdateDTO dto,
            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.updateUserBookById(userId, bookId, dto, parserUtil.ifMatchParser(ifMatch)), request);
    }
}
