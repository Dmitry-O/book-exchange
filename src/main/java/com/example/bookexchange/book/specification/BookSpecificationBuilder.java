package com.example.bookexchange.book.specification;

import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.book.model.Book;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class BookSpecificationBuilder {

    public static Specification<Book> build(BookSearchDTO dto, BookType bookType) {
        return build(dto, bookType, null);
    }

    public static Specification<Book> build(BookSearchDTO dto, BookType bookType, Long excludedUserId) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            predicate = cb.and(predicate, cb.equal(root.get("isExchanged"), false));

            if (excludedUserId != null) {
                predicate = cb.and(predicate, cb.notEqual(root.get("user").get("id"), excludedUserId));
            }

            if (dto.getAuthor() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("author"), dto.getAuthor()));
            }

            if (dto.getCategory() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category"), dto.getCategory().getProperty()));
            }

            if (dto.getCity() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("city"), dto.getCity()));
            }

            if (dto.getPublicationYear() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("publicationYear"), dto.getPublicationYear()));
            }

            if (dto.getIsGift() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("isGift"), dto.getIsGift()));
            }

            if (dto.getSearchText() != null && !dto.getSearchText().isBlank()) {
                String like = "%" + dto.getSearchText().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), like);
                Predicate descriptionLike = cb.like(cb.lower(root.get("description")), like);
                predicate = cb.and(predicate, cb.or(nameLike, descriptionLike));
            }

            if (bookType != BookType.ALL) {
                predicate = cb.and(predicate, bookType == BookType.ACTIVE ? cb.isNull(root.get("deletedAt")) : cb.isNotNull(root.get("deletedAt")));
            }

            return predicate;
        };
    }
}
