package com.example.bookexchange.config;

import com.example.bookexchange.user.mapper.UserMapper;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.book.service.BookService;
import com.example.bookexchange.exchange.service.RequestService;
import com.example.bookexchange.user.service.UserService;
import com.example.bookexchange.util.BookUtil;
import com.example.bookexchange.util.ExchangeUtilIT;
import com.example.bookexchange.util.UserUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestUserConfig {

    @Bean
    public UserUtil userUtil(UserService userService, UserMapper userMapper, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        return new UserUtil(userService, userMapper, passwordEncoder, userRepository);
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