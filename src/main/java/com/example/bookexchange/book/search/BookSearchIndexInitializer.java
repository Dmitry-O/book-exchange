package com.example.bookexchange.book.search;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.search.books", name = {"enabled", "reindex-on-startup"}, havingValue = "true")
public class BookSearchIndexInitializer {

    private final BookRepository bookRepository;
    private final BookSearchIndexService bookSearchIndexService;

    @EventListener(ApplicationReadyEvent.class)
    public void reindexOnStartup() {
        try {
            List<Book> books = bookRepository.findAll();
            Result<Void> result = bookSearchIndexService.reindexAll(books);

            if (result.isSuccess()) {
                log.info("Book Elasticsearch index reindexed on startup. bookCount={}", books.size());
                return;
            }

            log.warn("Book Elasticsearch startup reindex returned failure. bookCount={}", books.size());
        } catch (Exception ex) {
            log.warn("Book Elasticsearch startup reindex failed. reason={}", ex.getMessage(), ex);
        }
    }
}
