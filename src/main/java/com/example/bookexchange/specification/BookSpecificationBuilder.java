package com.example.bookexchange.specification;

import com.example.bookexchange.dto.BookSearchDTO;
import com.example.bookexchange.models.Book;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class BookSpecificationBuilder {

    public static Specification<Book> build(BookSearchDTO dto) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            predicate = cb.and(predicate, cb.equal(root.get("isExchanged"), false));

            if (dto.getAuthor() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("author"), dto.getAuthor()));
            }

            if (dto.getCategory() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category"), dto.getCategory()));
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
                String like = "%" +  dto.getSearchText() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), like);
                Predicate descriptionLike = cb.like(cb.lower(root.get("description")), like);
                predicate = cb.and(predicate, cb.or(nameLike, descriptionLike));
            }

            return predicate;
        };
    }
}
