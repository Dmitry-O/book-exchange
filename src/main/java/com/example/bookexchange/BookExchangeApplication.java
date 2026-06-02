package com.example.bookexchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "com.example.bookexchange.auth.repository",
        "com.example.bookexchange.book.repository",
        "com.example.bookexchange.common.city.repository",
        "com.example.bookexchange.common.notification",
        "com.example.bookexchange.exchange.repository",
        "com.example.bookexchange.report.repository",
        "com.example.bookexchange.user.repository"
})
@EnableElasticsearchRepositories(basePackages = "com.example.bookexchange.book.search")
@SpringBootApplication
@ConfigurationPropertiesScan
public class BookExchangeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookExchangeApplication.class, args);
    }

}
