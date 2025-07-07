package com.example.bookexchange.controllers;

import com.example.bookexchange.models.Book;
import com.example.bookexchange.services.BookService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/book")
public class BookController {
    private final BookService bookService;

    @RequestMapping(method = RequestMethod.GET)
    public Iterable<Book> getBooks() {
        return bookService.findAll();
    }

    @RequestMapping(value = "/{bookId}", method = RequestMethod.GET)
    public Book getBookById(@PathVariable("bookId") Long bookId) {
        return bookService.findById(bookId);
    }
}
