package com.example.bookexchange.repositories;

import com.example.bookexchange.models.Book;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends CrudRepository<Book, Long> {

    List<Book> findByUserIdAndIsExchanged(Long userId, Boolean isExchanged);

    List<Book> findAll(Specification<Book> specification, Sort sort);

    Optional<Book> findByIdAndUserId(Long bookId, Long userId);

    void deleteByIdAndUserId(Long bookId, Long userId);
}
