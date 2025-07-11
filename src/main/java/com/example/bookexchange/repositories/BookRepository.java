package com.example.bookexchange.repositories;

import com.example.bookexchange.models.Book;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends CrudRepository<Book, Long> {

    List<Book> findByUserId(Long userId);

    List<Book> findAll();

    Optional<Book> findByIdAndUserId(Long userId, Long bookId);

    void deleteByIdAndUserId(Long bookId, Long userId);
}
