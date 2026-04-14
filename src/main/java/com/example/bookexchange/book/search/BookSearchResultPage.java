package com.example.bookexchange.book.search;

import java.util.List;

public record BookSearchResultPage(
        List<Long> bookIds,
        long totalHits
) {

}
