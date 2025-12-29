package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.dto.BookSearchDTO;
import com.example.bookexchange.services.BookService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity addUserBook(@PathVariable Long userId, @RequestBody BookCreateDTO dto) {
        BookDTO savedBook = bookService.addUserBook(userId, dto);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, BOOK_PATH + "/" + userId + "/" + savedBook.getId().toString());

        return new ResponseEntity(headers, HttpStatus.CREATED);
    }

    @GetMapping(BOOK_PATH_USER_ID)
    public Page<BookDTO> getUserBooks(
            @PathVariable Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return bookService.findUserBooks(userId, pageIndex, pageSize);
    }

    @GetMapping(BOOK_PATH_HISTORY_USER_ID)
    public Page<BookDTO> getExchangedUserBooks(
            @PathVariable Long userId,
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
    public ResponseEntity deleteBookById(@PathVariable Long userId, @PathVariable Long bookId) {
        if (!bookService.deleteUserBookById(userId, bookId)) {
            throw new NotFoundException();
        }

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @PatchMapping(BOOK_PATH_USER_ID_BOOK_ID)
    public ResponseEntity updateUserBookById(@PathVariable Long userId, @PathVariable Long bookId, @RequestBody BookDTO dto) {
        if (bookService.updateUserBookById(userId, bookId, dto).isEmpty()) {
            throw new NotFoundException("Das Buch mit ID " + bookId + " oder mit user ID + " + userId + " wurde nicht gefunden");
        }

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
}
