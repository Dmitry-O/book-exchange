package com.example.bookexchange.controllers;

import com.example.bookexchange.config.TestUserConfig;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.mappers.BookMapper;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.ExchangeRepository;
import com.example.bookexchange.util.BookUtil;
import com.example.bookexchange.util.ExchangeUtilIT;
import com.example.bookexchange.util.UserUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.example.bookexchange.util.TestConstants.*;

@SpringBootTest
@Import(TestUserConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookControllerIT {

    @Autowired
    BookController bookController;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    ExchangeRepository exchangeRepository;

    @Autowired
    BookMapper bookMapper;

    @Autowired
    UserUtil userUtil;

    @Autowired
    BookUtil bookUtil;

    @Autowired
    ExchangeUtilIT exchangeUtilIT;

    @Autowired
    TransactionTemplate transactionTemplate;

    private Long userId;
    private Long bookId;

    @BeforeAll
    void init() {
        userId = userUtil.createUser(null);
        bookId = bookUtil.createBook(userId, null);
    }

    @AfterAll
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            bookUtil.deleteUserBooks(userId);
            userUtil.deleteUser(userId);
        });

        userId = null;
        bookId = null;
    }

    @Rollback
    @Transactional
    @Test
    void addUserBook() {
        BookCreateDTO bookCreateDTO = BookCreateDTO.builder()
                .name("Book 1")
                .description("Book 1")
                .author("Author 1")
                .category("Category 1")
                .publicationYear(2000)
                .photoBase64("photo.jpg")
                .city("City 1")
                .contactDetails("Contact Details 1")
                .isGift(false)
                .isExchanged(false)
                .build();

        ResponseEntity responseEntity = bookController.addUserBook(userId, bookCreateDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
        assertThat(responseEntity.getHeaders().getLocation()).isNotNull();

        String[] locationId = responseEntity.getHeaders().getLocation().getPath().split("/");
        Long savedId = Long.valueOf(locationId[5]);

        Book book = bookRepository.findById(savedId).get();

        assertThat(book).isNotNull();
    }

    @Rollback
    @Transactional
    @Test
    void getUserBooks() {
        Page<BookDTO> books = bookController.getUserBooks(userId, PAGE_INDEX, PAGE_SIZE);

        assertThat(books.getTotalElements()).isEqualTo(1);
    }

    @Rollback
    @Transactional
    @Test
    void getUserBooksNotFound() {
        bookRepository.deleteByUserId(userId);

        Page<BookDTO> books = bookController.getUserBooks(userId, PAGE_INDEX, PAGE_SIZE);

        assertThat(books.getTotalElements()).isEqualTo(0);
    }

    @Rollback
    @Transactional
    @Test
    void getExchangedUserBooks() {
        Long senderUserId = userUtil.createUser(2);
        Long receiverUserId = userUtil.createUser(3);

        Long senderBookId = bookUtil.createBook(senderUserId, 2);
        Long receiverBookId = bookUtil.createBook(receiverUserId, 3);

        Long exchangeId = exchangeUtilIT.createExchange(senderUserId, receiverUserId, senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();
        Book book = bookRepository.findById(senderBookId).get();

        exchange.setStatus(ExchangeStatus.APPROVED);
        book.setIsExchanged(true);

        exchangeRepository.save(exchange);
        bookRepository.save(book);

        Page<BookDTO> exchangedUserBookDTOs = bookController.getExchangedUserBooks(senderUserId, PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangedUserBookDTOs.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getExchangedUserBooksNotFound() {
        Page<BookDTO> exchangedUserBookDTOs = bookController.getExchangedUserBooks(userId, PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangedUserBookDTOs.getTotalElements()).isEqualTo(0);
    }

    @Rollback
    @Transactional
    @Test
    void getBooks() {
        BookSearchDTO bookSearchDTO = BookSearchDTO.builder()
                .author("Author 1")
                .category("Category 1")
                .publicationYear(2000)
                .city("City 1")
                .isGift(false)
                .searchText("Book 1")
                .sortBy("category")
                .sortDirection("asc")
                .build();

        Page<BookDTO> books = bookController.getBooks(PAGE_INDEX, PAGE_SIZE, bookSearchDTO);

        assertThat(books.getTotalElements()).isEqualTo(1);
    }

    @Rollback
    @Transactional
    @Test
    void getBooksExtended() {
        bookUtil.createSeveralBooks(userId, NUMBER_OF_SAME_BOOKS, NUMBER_OF_DIFFERENT_BOOKS);

        BookSearchDTO bookSearchDTO = BookSearchDTO.builder()
                .author("Author 1")
                .category("Category 1")
                .publicationYear(2000)
                .city("City 1")
                .isGift(false)
                .searchText("Book 1")
                .sortBy("category")
                .sortDirection("asc")
                .build();

        Page<BookDTO> books = bookController.getBooks(PAGE_INDEX, PAGE_SIZE, bookSearchDTO);

        assertThat(books.getTotalElements()).isEqualTo(NUMBER_OF_SAME_BOOKS + 1);
    }

    @Rollback
    @Transactional
    @Test
    void getBooksNotFound() {
        bookRepository.deleteAll();

        Page<BookDTO> books = bookController.getBooks(PAGE_INDEX, PAGE_SIZE, BookSearchDTO.builder().build());

        assertThat(books.getTotalElements()).isEqualTo(0);
    }

    @Rollback
    @Transactional
    @Test
    void deleteBookById() {
        Book book = bookRepository.findById(bookId).get();

        ResponseEntity responseEntity = bookController.deleteBookById(userId, book.getId());

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));
        assertThat(bookRepository.findById(book.getId())).isEmpty();
    }

    @Rollback
    @Transactional
    @Test
    void deleteBookByIdNotFound() {
        assertThrows(NotFoundException.class, () -> {
            bookController.deleteBookById(userId, System.nanoTime());
        });
    }

    @Rollback
    @Transactional
    @Test
    void updateUserBookById() {
        Book book = bookRepository.findAll().get(0);
        BookDTO bookDTO = bookMapper.bookToBookDto(book);

        final String bookName = "Book 2";

        bookDTO.setId(null);
        bookDTO.setName(bookName);

        ResponseEntity responseEntity = bookController.updateUserBookById(userId, book.getId(), bookDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        Book updatedBook = bookRepository.findById(book.getId()).get();

        assertThat(updatedBook.getName()).isEqualTo(bookName);
    }

    @Test
    void updateUserBookByIdNotFound() {
        assertThrows(NotFoundException.class, () -> {
            bookController.updateUserBookById(userId, System.nanoTime(), BookDTO.builder().build());
        });
    }
}