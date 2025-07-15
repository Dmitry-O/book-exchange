package com.example.bookexchange.repositories;

import com.example.bookexchange.models.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    Page<Book> findByUserIdAndIsExchanged(Long userId, Boolean isExchanged, Pageable pageable);

    Page<Book> findAll(Specification<Book> specification, Pageable pageable);

    Optional<Book> findByIdAndUserId(Long bookId, Long userId);

    void deleteByIdAndUserId(Long bookId, Long userId);
}
