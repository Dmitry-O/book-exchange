package com.example.bookexchange.book.search;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.search.books")
public class BookSearchProperties {

    private boolean enabled = false;
    private boolean reindexOnStartup = true;
    private int minSearchLength = 3;
}
