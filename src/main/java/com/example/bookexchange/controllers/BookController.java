package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.User;
import com.example.bookexchange.services.BookService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class BookController {

    public static final String BOOK_PATH = "/api/v1/book";
    public static final String BOOK_PATH_USER = BOOK_PATH + "/user";
    public static final String BOOK_PATH_HISTORY = BOOK_PATH + "/history";
    public static final String BOOK_PATH_USER_BOOK_ID = BOOK_PATH_USER + "/{bookId}";

    private final BookService bookService;

    @PostMapping(BOOK_PATH_USER)
    public ResponseEntity<String> addUserBook(@AuthenticationPrincipal User user, @Validated @RequestBody BookCreateDTO dto) {
        BookDTO savedBook = bookService.addUserBook(user.getId(), dto);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, BOOK_PATH + "/" + user.getId() + "/" + savedBook.getId().toString());

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @GetMapping(BOOK_PATH_USER)
    public Page<BookDTO> getUserBooks(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return bookService.findUserBooks(user.getId(), pageIndex, pageSize);
    }

    @GetMapping(BOOK_PATH_HISTORY)
    public Page<BookDTO> getExchangedUserBooks(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return bookService.findExchangedUserBooks(user.getId(), pageIndex, pageSize);
    }

    @GetMapping(BOOK_PATH)
    public Page<BookDTO> getBooks(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @Validated @RequestBody(required = false) BookSearchDTO dto
    ) {
        return bookService.findBooks(dto, pageIndex, pageSize);
    }

    @DeleteMapping(BOOK_PATH_USER_BOOK_ID)
    public ResponseEntity<String> deleteBookById(@AuthenticationPrincipal User user, @PathVariable Long bookId) {
        if (!bookService.deleteUserBookById(user.getId(), bookId)) {
            throw new NotFoundException();
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping(BOOK_PATH_USER_BOOK_ID)
    public ResponseEntity<String> updateUserBookById(@AuthenticationPrincipal User user, @PathVariable Long bookId, @Validated @RequestBody BookUpdateDTO dto) {
        if (bookService.updateUserBookById(user.getId(), bookId, dto).isEmpty()) {
            throw new NotFoundException("Das Buch mit ID " + bookId + " oder mit user ID + " + user.getId() + " wurde nicht gefunden");
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
