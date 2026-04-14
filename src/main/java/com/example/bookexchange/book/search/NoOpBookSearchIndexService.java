package com.example.bookexchange.book.search;

import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "app.search.books", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpBookSearchIndexService implements BookSearchIndexService {

    @Override
    public Optional<BookSearchResultPage> search(BookSearchDTO dto, PageQueryDTO queryDTO, BookType bookType) {
        return Optional.empty();
    }

    @Override
    public void scheduleUpsert(Book book) {
    }

    @Override
    public void scheduleUpsertAll(Collection<Book> books) {
    }

    @Override
    public Result<Void> reindexAll(Collection<Book> books) {
        return ResultFactory.error(MessageKey.ADMIN_BOOK_SEARCH_DISABLED, HttpStatus.BAD_REQUEST);
    }
}
