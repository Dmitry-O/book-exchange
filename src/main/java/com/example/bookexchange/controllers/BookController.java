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
@RequestMapping("/api/v1/book")
public class BookController {

    private final BookService bookService;

    @PostMapping("/{userId}")
    public BookDTO addUserBook(@PathVariable("userId") Long userId, @RequestBody BookCreateDTO dto) {
        return bookService.addUserBook(userId, dto);
    }

    @GetMapping("/{userId}")
    public Page<BookDTO> getUserBooks(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return bookService.findUserBooks(userId, pageIndex, pageSize);
    }

    @GetMapping("/history/{userId}")
    public Page<BookDTO> getExchangedUserBooks(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return bookService.findExchangedUserBooks(userId, pageIndex, pageSize);
    }

    @GetMapping()
    public Page<BookDTO> getBooks(
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestBody(required = false) BookSearchDTO dto
    ) {
        return bookService.findBooks(dto, pageIndex, pageSize);
    }

    @DeleteMapping("/{userId}/{bookId}")
    public String deleteBookById(@PathVariable("userId") Long userId, @PathVariable("bookId") Long bookId) {
        return bookService.deleteUserBookById(userId, bookId);
    }

    @PatchMapping("/{userId}/{bookId}")
    public String updateUserBookById(@PathVariable("userId") Long userId, @PathVariable("bookId") Long bookId, @RequestBody BookDTO dto) {
        return bookService.updateUserBookById(userId, bookId, dto);
    }
}
