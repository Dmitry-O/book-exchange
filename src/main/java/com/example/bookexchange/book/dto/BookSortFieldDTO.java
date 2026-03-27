package com.example.bookexchange.book.dto;

public enum BookSortFieldDTO {
    NAME("name"),
    AUTHOR("author"),
    CATEGORY("category"),
    PUBLICATION_YEAR("publicationYear"),
    CITY("city"),
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt");

    private final String property;

    BookSortFieldDTO(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }
}
