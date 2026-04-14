package com.example.bookexchange.book.search;

import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.dto.BookSortFieldDTO;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.dto.SortDirectionDTO;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.search.books", name = "enabled", havingValue = "true")
public class ElasticsearchBookSearchIndexService implements BookSearchIndexService {

    private static final IndexCoordinates INDEX_COORDINATES = IndexCoordinates.of("book-search-books");

    private final ElasticsearchOperations elasticsearchOperations;
    private final BookSearchDocumentRepository bookSearchDocumentRepository;
    private final ObjectMapper objectMapper;
    private final BookSearchProperties bookSearchProperties;

    @Override
    public Optional<BookSearchResultPage> search(BookSearchDTO dto, PageQueryDTO queryDTO, BookType bookType) {
        if (!supports(dto)) {
            return Optional.empty();
        }

        try {
            ensureIndexExists();

            StringQuery query = createQuery(dto, queryDTO, bookType);
            SearchHits<BookSearchDocument> searchHits = elasticsearchOperations.search(query, BookSearchDocument.class, INDEX_COORDINATES);

            List<Long> ids = searchHits.getSearchHits().stream()
                    .map(hit -> hit.getContent().id())
                    .toList();

            return Optional.of(new BookSearchResultPage(ids, searchHits.getTotalHits()));
        } catch (Exception ex) {
            log.warn(
                    "Book Elasticsearch search failed. Falling back to JPA search. reason={}",
                    ex.getMessage(),
                    ex
            );

            return Optional.empty();
        }
    }

    @Override
    public void scheduleUpsert(Book book) {
        if (book == null) {
            return;
        }

        scheduleAfterCommit(() -> upsertNow(book));
    }

    @Override
    public void scheduleUpsertAll(Collection<Book> books) {
        if (books == null || books.isEmpty()) {
            return;
        }

        List<Book> snapshot = new ArrayList<>(books);
        scheduleAfterCommit(() -> upsertAllNow(snapshot));
    }

    @Override
    public Result<Void> reindexAll(Collection<Book> books) {
        try {
            ensureIndexExists();
            bookSearchDocumentRepository.deleteAll();
            upsertAllNow(books);
            int bookCount = books == null ? 0 : books.size();
            return ResultFactory.okMessage(MessageKey.ADMIN_BOOK_SEARCH_REINDEXED, bookCount);
        } catch (Exception ex) {
            log.warn(
                    "Book Elasticsearch reindex failed. reason={}",
                    ex.getMessage(),
                    ex
            );
            return ResultFactory.error(MessageKey.SYSTEM_UNEXPECTED_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean supports(BookSearchDTO dto) {
        return dto != null
                && dto.getSearchText() != null
                && dto.getSearchText().trim().length() >= bookSearchProperties.getMinSearchLength();
    }

    private void ensureIndexExists() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(BookSearchDocument.class);

        if (!indexOperations.exists()) {
            indexOperations.createWithMapping();
        }
    }

    private StringQuery createQuery(BookSearchDTO dto, PageQueryDTO queryDTO, BookType bookType) throws JsonProcessingException {
        String source = objectMapper.writeValueAsString(buildQuerySource(dto, bookType));
        Sort sort = mapSort(dto);

        if (sort == null) {
            return new StringQuery(source, PageRequest.of(queryDTO.getPageIndex(), queryDTO.getPageSize()));
        }

        return new StringQuery(source, PageRequest.of(queryDTO.getPageIndex(), queryDTO.getPageSize()), sort);
    }

    private Map<String, Object> buildQuerySource(BookSearchDTO dto, BookType bookType) {
        List<Object> shouldClauses = List.of(
                Map.of(
                        "multi_match",
                        Map.of(
                                "query", dto.getSearchText(),
                                "fields", List.of("name^6", "author^4", "category^3", "city^2", "description"),
                                "type", "best_fields",
                                "fuzziness", "AUTO"
                        )
                ),
                Map.of(
                        "multi_match",
                        Map.of(
                                "query", dto.getSearchText(),
                                "fields", List.of("name^10", "author^6", "category^4"),
                                "type", "phrase_prefix",
                                "boost", 3
                        )
                )
        );

        List<Object> filters = new ArrayList<>();
        filters.add(term("isExchanged", false));

        if (bookType == BookType.ACTIVE) {
            filters.add(term("deleted", false));
        } else if (bookType == BookType.DELETED) {
            filters.add(term("deleted", true));
        }

        addKeywordFilter(filters, "authorSort", dto.getAuthor());
        addKeywordFilter(filters, "categorySort", dto.getCategory() != null ? dto.getCategory().getProperty() : null);
        addKeywordFilter(filters, "citySort", dto.getCity());

        if (dto.getPublicationYear() != null) {
            filters.add(term("publicationYear", dto.getPublicationYear()));
        }

        if (dto.getIsGift() != null) {
            filters.add(term("isGift", dto.getIsGift()));
        }

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put(
                "must",
                List.of(
                        Map.of(
                                "bool",
                                Map.of(
                                        "should", shouldClauses,
                                        "minimum_should_match", 1
                                )
                        )
                )
        );
        bool.put("filter", filters);

        return Map.of("bool", bool);
    }

    private Sort mapSort(BookSearchDTO dto) {
        if (dto.getSortBy() == null || dto.getSortDirection() == null) {
            return null;
        }

        Sort.Direction direction = dto.getSortDirection() == SortDirectionDTO.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        String sortField = mapSortField(dto.getSortBy());

        return Sort.by(direction, sortField).and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private String mapSortField(BookSortFieldDTO sortField) {
        return switch (sortField) {
            case NAME -> "nameSort";
            case AUTHOR -> "authorSort";
            case CATEGORY -> "categorySort";
            case CITY -> "citySort";
            case PUBLICATION_YEAR -> "publicationYear";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
        };
    }

    private Map<String, Object> term(String field, Object value) {
        return Map.of("term", Map.of(field, value));
    }

    private void addKeywordFilter(List<Object> filters, String field, String value) {
        if (value != null && !value.isBlank()) {
            filters.add(term(field, value.trim().toLowerCase(Locale.ROOT)));
        }
    }

    private void upsertNow(Book book) {
        try {
            ensureIndexExists();
            bookSearchDocumentRepository.save(BookSearchDocument.fromBook(book));
        } catch (Exception ex) {
            log.warn(
                    "Failed to upsert book document in Elasticsearch. bookId={}, reason={}",
                    book.getId(),
                    ex.getMessage(),
                    ex
            );
        }
    }

    private void upsertAllNow(Collection<Book> books) {
        if (books == null || books.isEmpty()) {
            return;
        }

        try {
            ensureIndexExists();
            bookSearchDocumentRepository.saveAll(
                    books.stream()
                            .map(BookSearchDocument::fromBook)
                            .toList()
            );
        } catch (Exception ex) {
            log.warn(
                    "Failed to upsert book documents in Elasticsearch. bookCount={}, reason={}",
                    books.size(),
                    ex.getMessage(),
                    ex
            );
        }
    }

    private void scheduleAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }

        action.run();
    }
}
