package com.example.bookexchange.services;

import com.example.bookexchange.models.Book;
import com.example.bookexchange.repositories.BookRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Override
    public Iterable<Book> findAll() {
        return bookRepository.findAll();
    }

    @Override
    public Book findById(Long bookId) {
        Optional<Book> book = bookRepository.findById(bookId);

        if (book.isEmpty()) {
            throw new IllegalArgumentException("Buch mit ID " + bookId + " wurde nicht gefunden");
        }

        return book.get();
    }
}
