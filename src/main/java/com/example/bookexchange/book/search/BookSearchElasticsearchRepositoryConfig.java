package com.example.bookexchange.book.search;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.search.books", name = "enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackageClasses = BookSearchDocumentRepository.class)
public class BookSearchElasticsearchRepositoryConfig {
}
