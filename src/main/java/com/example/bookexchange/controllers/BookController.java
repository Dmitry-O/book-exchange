package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.services.BookService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public List<BookDTO> getUserBooks(@PathVariable("userId") Long userId) {
        return bookService.findUserBooks(userId);
    }

    @GetMapping()
    public List<BookDTO> getBooks() {
        return bookService.findBooks();
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
