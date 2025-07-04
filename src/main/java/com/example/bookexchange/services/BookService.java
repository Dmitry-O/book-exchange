package com.example.bookexchange.services;

import com.example.bookexchange.models.Book;

public interface BookService {

    Iterable<Book> findAll();

    Book findById(Long bookId);
}
