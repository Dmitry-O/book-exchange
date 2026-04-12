package com.example.bookexchange.config;

import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.email.EmailService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.common.web.ErrorResponseWriter;
import com.example.bookexchange.user.mapper.UserMapper;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.book.service.BookService;
import com.example.bookexchange.exchange.service.RequestService;
import com.example.bookexchange.security.filter.RateLimitFilter;
import com.example.bookexchange.user.service.UserService;
import com.example.bookexchange.support.fixture.BookFixtureSupport;
import com.example.bookexchange.support.fixture.ExchangeFixtureSupport;
import com.example.bookexchange.support.fixture.UserFixtureSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;

@TestConfiguration
public class TestUserConfig {

    @Bean
    public UserFixtureSupport userFixtureSupport(UserService userService, UserMapper userMapper, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        return new UserFixtureSupport(userService, userMapper, passwordEncoder, userRepository);
    }

    @Bean
    public BookFixtureSupport bookFixtureSupport(BookService bookService, BookRepository bookRepository) {
        return new BookFixtureSupport(bookService, bookRepository);
    }

    @Bean
    public ExchangeFixtureSupport exchangeFixtureSupport(RequestService requestService) {
        return new ExchangeFixtureSupport(requestService);
    }

    @Bean
    @Primary
    public EmailService testEmailService() {
        return (emailTo, token, emailType) -> ResultFactory.successVoid();
    }

    @Bean
    @Primary
    public RateLimitFilter testRateLimitFilter(ErrorResponseWriter errorResponseWriter, AuditService auditService) {
        return new RateLimitFilter(errorResponseWriter, auditService) {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain
            ) throws ServletException, IOException {
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    @Primary
    public ImageStorageService testImageStorageService() {
        return new ImageStorageService() {
            @Override
            public Result<String> replaceUserProfileImage(Long userId, String photoBase64) {
                return ResultFactory.ok("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + userId + "/profile_photo_test.jpg");
            }

            @Override
            public Result<String> replaceBookImage(Long userId, Long bookId, String photoBase64) {
                return ResultFactory.ok("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + userId + "/books/" + bookId + "_test.jpg");
            }

            @Override
            public Result<Void> deleteUserProfileImage(Long userId) {
                return ResultFactory.successVoid();
            }

            @Override
            public Result<Void> deleteBookImage(Long userId, Long bookId) {
                return ResultFactory.successVoid();
            }

            @Override
            public Result<Void> deleteAllUserImages(Long userId) {
                return ResultFactory.successVoid();
            }
        };
    }
}
