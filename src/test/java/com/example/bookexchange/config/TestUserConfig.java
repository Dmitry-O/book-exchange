package com.example.bookexchange.config;

import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.services.BookService;
import com.example.bookexchange.services.RequestService;
import com.example.bookexchange.services.UserService;
import com.example.bookexchange.util.BookUtil;
import com.example.bookexchange.util.ExchangeUtilIT;
import com.example.bookexchange.util.UserUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestUserConfig {

    @Bean
    public UserUtil userUtil(UserService userService) {
        return new UserUtil(userService);
    }

    @Bean
    public BookUtil bookUtil(BookService bookService, BookRepository bookRepository) {
        return new BookUtil(bookService, bookRepository);
    }

    @Bean
    public ExchangeUtilIT exchangeUtilIT(RequestService requestService) {
        return new ExchangeUtilIT(requestService);
    }
}