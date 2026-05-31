package com.example.bookexchange.book.repository;

import com.example.bookexchange.book.model.Book;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    @EntityGraph(attributePaths = "user")
    Page<Book> findByUserIdAndIsExchanged(Long userId, Boolean isExchanged, Pageable pageable);

    @NullMarked
    @EntityGraph(attributePaths = "user")
    Page<Book> findAll(Specification<Book> specification, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Optional<Book> findByIdAndUserId(Long bookId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<Book> findAllByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT b
            FROM Book b
            WHERE b.id = :bookId
                AND b.deletedAt IS NULL
                AND b.isExchanged = false
            """
    )
    Optional<Book> findPublicBookById(@Param("bookId") Long bookId);

    List<Book> findAllByUserIdAndDeletedAtIsNull(Long userId);

    @EntityGraph(attributePaths = "user")
    List<Book> findAllByDeletedAtBeforeAndPhotoUrlIsNotNull(Instant deletedAtBefore);

    long countByDeletedAtIsNullAndIsExchangedFalse();

    @Query("""
            SELECT b.category AS category, COUNT(b.id) AS bookCount
            FROM Book b
            WHERE b.deletedAt IS NULL
                AND b.isExchanged = false
            GROUP BY b.category
            ORDER BY COUNT(b.id) DESC, b.category ASC
            """
    )
    List<BookCategoryCountView> countAvailableBooksByCategory();

    void deleteByUserId(Long userId);

    interface BookCategoryCountView {

        String getCategory();

        long getBookCount();
    }
}
