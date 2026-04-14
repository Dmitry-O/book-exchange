package com.example.bookexchange.book.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BookSearchDocumentRepository extends ElasticsearchRepository<BookSearchDocument, Long> {

}
