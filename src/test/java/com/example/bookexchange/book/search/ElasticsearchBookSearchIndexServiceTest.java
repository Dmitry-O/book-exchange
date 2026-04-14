package com.example.bookexchange.book.search;

import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.dto.BookSortFieldDTO;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.dto.SortDirectionDTO;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.StringQuery;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticsearchBookSearchIndexServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private BookSearchDocumentRepository bookSearchDocumentRepository;

    @Mock
    private IndexOperations indexOperations;

    private ElasticsearchBookSearchIndexService service;

    @BeforeEach
    void setUp() {
        BookSearchProperties properties = new BookSearchProperties();
        properties.setEnabled(true);
        properties.setMinSearchLength(3);

        service = new ElasticsearchBookSearchIndexService(
                elasticsearchOperations,
                bookSearchDocumentRepository,
                new ObjectMapper(),
                properties
        );
    }

    @Test
    void shouldReturnBookIdsFromElasticsearch_whenSearchTextIsValid() {
        BookSearchDTO dto = BookSearchDTO.builder()
                .searchText("char")
                .author("Frank Oester")
                .sortBy(BookSortFieldDTO.NAME)
                .sortDirection(SortDirectionDTO.ASC)
                .build();
        PageQueryDTO queryDTO = new PageQueryDTO();

        @SuppressWarnings("unchecked")
        SearchHit<BookSearchDocument> firstHit = mock(SearchHit.class);
        @SuppressWarnings("unchecked")
        SearchHit<BookSearchDocument> secondHit = mock(SearchHit.class);
        @SuppressWarnings("unchecked")
        SearchHits<BookSearchDocument> searchHits = mock(SearchHits.class);

        when(elasticsearchOperations.indexOps(BookSearchDocument.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);
        when(firstHit.getContent()).thenReturn(BookSearchDocument.fromBook(
                UnitTestDataFactory.book(
                        10L,
                        "Charley Smash",
                        UnitTestDataFactory.user(1L, "owner@example.com", "owner")
                )
        ));
        when(secondHit.getContent()).thenReturn(BookSearchDocument.fromBook(
                UnitTestDataFactory.book(
                        11L,
                        "Smash Hits",
                        UnitTestDataFactory.user(2L, "owner2@example.com", "owner2")
                )
        ));
        when(searchHits.getSearchHits()).thenReturn(List.of(firstHit, secondHit));
        when(searchHits.getTotalHits()).thenReturn(2L);
        when(elasticsearchOperations.search(any(StringQuery.class), eq(BookSearchDocument.class), any(IndexCoordinates.class)))
                .thenReturn(searchHits);

        Optional<BookSearchResultPage> result = service.search(dto, queryDTO, BookType.ACTIVE);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().bookIds()).containsExactly(10L, 11L);
        assertThat(result.orElseThrow().totalHits()).isEqualTo(2L);

        ArgumentCaptor<StringQuery> queryCaptor = ArgumentCaptor.forClass(StringQuery.class);
        verify(elasticsearchOperations).search(queryCaptor.capture(), eq(BookSearchDocument.class), any(IndexCoordinates.class));
        assertThat(queryCaptor.getValue().getSource()).contains("\"query\":\"char\"");
        assertThat(queryCaptor.getValue().getSource()).contains("\"authorSort\":\"frank oester\"");
        assertThat(queryCaptor.getValue().getSource()).contains("\"deleted\":false");
    }

    @Test
    void shouldFallBackWhenElasticsearchSearchFails() {
        BookSearchDTO dto = BookSearchDTO.builder()
                .searchText("char")
                .build();
        PageQueryDTO queryDTO = new PageQueryDTO();

        when(elasticsearchOperations.indexOps(BookSearchDocument.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);
        when(elasticsearchOperations.search(any(StringQuery.class), eq(BookSearchDocument.class), any(IndexCoordinates.class)))
                .thenThrow(new RuntimeException("cluster unavailable"));

        Optional<BookSearchResultPage> result = service.search(dto, queryDTO, BookType.ACTIVE);

        assertThat(result).isEmpty();
    }
}
