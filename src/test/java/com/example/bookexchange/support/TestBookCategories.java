package com.example.bookexchange.support;

import com.example.bookexchange.book.dto.BookCategoryDTO;

public final class TestBookCategories {

    private static final BookCategoryDTO[] CATEGORIES = {
            BookCategoryDTO.DRAMA,
            BookCategoryDTO.FANTASY,
            BookCategoryDTO.HISTORY,
            BookCategoryDTO.SCIENCE_FICTION,
            BookCategoryDTO.NOVEL,
            BookCategoryDTO.BUSINESS,
            BookCategoryDTO.THRILLER,
            BookCategoryDTO.PSYCHOLOGY,
            BookCategoryDTO.ROMANCE,
            BookCategoryDTO.TECHNOLOGY
    };

    private TestBookCategories() {
    }

    public static BookCategoryDTO category(int number) {
        return CATEGORIES[Math.floorMod(number, CATEGORIES.length)];
    }

    public static BookCategoryDTO updatedCategory(int number) {
        return CATEGORIES[Math.floorMod(number + 3, CATEGORIES.length)];
    }
}
