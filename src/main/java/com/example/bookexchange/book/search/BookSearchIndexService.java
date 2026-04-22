package com.example.bookexchange.book.search;

import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.result.Result;

import java.util.Collection;
import java.util.Optional;

public interface BookSearchIndexService {

    Optional<BookSearchResultPage> search(Long currentUserId, BookSearchDTO dto, PageQueryDTO queryDTO, BookType bookType);

    void scheduleUpsert(Book book);

    void scheduleUpsertAll(Collection<Book> books);

    Result<Void> reindexAll(Collection<Book> books);
}
