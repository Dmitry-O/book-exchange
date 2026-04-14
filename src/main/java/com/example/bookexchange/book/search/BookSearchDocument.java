package com.example.bookexchange.book.search;

import com.example.bookexchange.book.model.Book;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.Instant;
import java.util.Locale;

@Document(indexName = "book-search-books", createIndex = false)
@Setting(settingPath = "/elasticsearch/book-search-settings.json")
public record BookSearchDocument(
        @Id
        Long id,

        @Field(type = FieldType.Text, analyzer = "book_ngram_analyzer", searchAnalyzer = "book_search_analyzer")
        String name,

        @Field(type = FieldType.Text, analyzer = "book_ngram_analyzer", searchAnalyzer = "book_search_analyzer")
        String author,

        @Field(type = FieldType.Text, analyzer = "book_ngram_analyzer", searchAnalyzer = "book_search_analyzer")
        String category,

        @Field(type = FieldType.Text, analyzer = "book_ngram_analyzer", searchAnalyzer = "book_search_analyzer")
        String city,

        @Field(type = FieldType.Text, analyzer = "book_ngram_analyzer", searchAnalyzer = "book_search_analyzer")
        String description,

        @Field(type = FieldType.Keyword, normalizer = "lowercase_normalizer")
        String nameSort,

        @Field(type = FieldType.Keyword, normalizer = "lowercase_normalizer")
        String authorSort,

        @Field(type = FieldType.Keyword, normalizer = "lowercase_normalizer")
        String categorySort,

        @Field(type = FieldType.Keyword, normalizer = "lowercase_normalizer")
        String citySort,

        @Field(type = FieldType.Integer)
        Integer publicationYear,

        @Field(type = FieldType.Boolean)
        Boolean isGift,

        @Field(type = FieldType.Boolean)
        Boolean isExchanged,

        @Field(type = FieldType.Boolean)
        Boolean deleted,

        @Field(type = FieldType.Date, format = DateFormat.date_time)
        Instant createdAt,

        @Field(type = FieldType.Date, format = DateFormat.date_time)
        Instant updatedAt,

        @Field(type = FieldType.Date, format = DateFormat.date_time)
        Instant deletedAt
) {

    public static BookSearchDocument fromBook(Book book) {
        return new BookSearchDocument(
                book.getId(),
                book.getName(),
                book.getAuthor(),
                book.getCategory(),
                book.getCity(),
                book.getDescription(),
                normalize(book.getName()),
                normalize(book.getAuthor()),
                normalize(book.getCategory()),
                normalize(book.getCity()),
                book.getPublicationYear(),
                Boolean.TRUE.equals(book.getIsGift()),
                Boolean.TRUE.equals(book.getIsExchanged()),
                book.getDeletedAt() != null,
                book.getCreatedAt(),
                book.getUpdatedAt(),
                book.getDeletedAt()
        );
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
