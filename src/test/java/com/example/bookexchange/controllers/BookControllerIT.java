package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.exception.NotFoundException;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.ExchangeRepository;
import com.example.bookexchange.util.BookUtil;
import com.example.bookexchange.util.ExchangeUtilIT;
import com.example.bookexchange.util.UserUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.example.bookexchange.util.TestConstants.*;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class BookControllerIT extends AbstractIT {

    @Autowired
    BookController bookController;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    ExchangeRepository exchangeRepository;

    @Autowired
    UserUtil userUtil;

    @Autowired
    BookUtil bookUtil;

    @Autowired
    ExchangeUtilIT exchangeUtilIT;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    ObjectMapper objectMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private User user;
    private Long bookId;

    @BeforeAll
    void init() {
        user = userUtil.createUser(null);
        bookId = bookUtil.createBook(user.getId(), null);
    }

    @AfterAll
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            bookUtil.deleteUserBooks(user.getId());
            userUtil.deleteUser(user.getId());
        });

        user = null;
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

        ResponseEntity<String> responseEntity = bookController.addUserBook(user.getId(), bookCreateDTO);

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
    void addUserBookBadRequest() throws Exception {
        BookCreateDTO bookCreateDTO = BookCreateDTO.builder().build();

        MvcResult mvcResult = mockMvc.perform(post(BookController.BOOK_PATH_USER, user)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookCreateDTO))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()", is(13)))
                .andReturn();

        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Rollback
    @Transactional
    @Test
    void getUserBooks() {
        Page<BookDTO> books = bookController.getUserBooks(user.getId(), PAGE_INDEX, PAGE_SIZE);

        assertThat(books.getTotalElements()).isEqualTo(1);
    }

    @Rollback
    @Transactional
    @Test
    void getUserBooksNotFound() {
        bookRepository.deleteByUserId(user.getId());

        Page<BookDTO> books = bookController.getUserBooks(user.getId(), PAGE_INDEX, PAGE_SIZE);

        assertThat(books.getTotalElements()).isEqualTo(0);
    }

    //TODO: To be refactored
//    @Test
//    void getUserBooksNotFound() throws Exception {
//        bookRepository.deleteByUserId(userId);
//
//        mockMvc.perform(get("/api/v1/books")
//                        .param("userId", userId.toString())
//                        .with(jwt())) // если есть security
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.totalElements").value(0));
//    }

    @Rollback
    @Transactional
    @Test
    void getExchangedUserBooks() {
        User senderUser = userUtil.createUser(2);
        User receiverUser = userUtil.createUser(3);

        Long senderBookId = bookUtil.createBook(senderUser.getId(), 2);
        Long receiverBookId = bookUtil.createBook(receiverUser.getId(), 3);

        Long exchangeId = exchangeUtilIT.createExchange(senderUser.getId(), receiverUser.getId(), senderBookId, receiverBookId);

        Exchange exchange = exchangeRepository.findById(exchangeId).get();
        Book book = bookRepository.findById(senderBookId).get();

        exchange.setStatus(ExchangeStatus.APPROVED);
        book.setIsExchanged(true);

        exchangeRepository.save(exchange);
        bookRepository.save(book);

        Page<BookDTO> exchangedUserBookDTOs = bookController.getExchangedUserBooks(senderUser.getId(), PAGE_INDEX, PAGE_SIZE);

        assertThat(exchangedUserBookDTOs.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getExchangedUserBooksNotFound() {
        Page<BookDTO> exchangedUserBookDTOs = bookController.getExchangedUserBooks(user.getId(), PAGE_INDEX, PAGE_SIZE);

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

    @Test
    void getBooksBadRequest() throws Exception {
        BookSearchDTO bookSearchDTO = BookSearchDTO.builder()
                .author("Author 1")
                .category("Category 1")
                .publicationYear(2000)
                .city("City 1")
                .isGift(false)
                .searchText("Book 012345678901234567890123456789")
                .sortBy("category")
                .sortDirection("asc")
                .build();

        MvcResult mvcResult = mockMvc.perform(get(BookController.BOOK_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("pageIndex", PAGE_INDEX.toString())
                        .queryParam("pageSize", PAGE_SIZE.toString())
                        .content(objectMapper.writeValueAsString(bookSearchDTO))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()", is(1)))
                .andReturn();

        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Rollback
    @Transactional
    @Test
    void getBooksExtended() {
        bookUtil.createSeveralBooks(user.getId(), NUMBER_OF_SAME_BOOKS, NUMBER_OF_DIFFERENT_BOOKS);

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

        ResponseEntity<String> responseEntity = bookController.deleteBookById(user.getId(), book.getId());

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));
        assertThat(bookRepository.findById(book.getId())).isEmpty();
    }

    @Rollback
    @Transactional
    @Test
    void deleteBookByIdNotFound() {
        assertThrows(NotFoundException.class, () -> {
            bookController.deleteBookById(user.getId(), System.nanoTime());
        });
    }

    @Rollback
    @Transactional
    @Test
    void updateUserBookById() {
        final String bookName = "Book 2";

        Book book = bookRepository.findAll().getFirst();

        BookUpdateDTO bookUpdateDTO = BookUpdateDTO.builder()
                .name(bookName)
                .description(book.getDescription())
                .author(book.getAuthor())
                .category(book.getCategory())
                .publicationYear(book.getPublicationYear())
                .photoBase64(book.getPhotoBase64())
                .city(book.getCity())
                .contactDetails(book.getContactDetails())
                .isGift(book.getIsGift())
                .build();

        ResponseEntity<String> responseEntity = bookController.updateUserBookById(user.getId(), book.getId(), bookUpdateDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        Book updatedBook = bookRepository.findById(book.getId()).get();

        assertThat(updatedBook.getName()).isEqualTo(bookName);
    }

    @Test
    void updateUserBookByIdNotFound() {
        assertThrows(NotFoundException.class, () -> bookController.updateUserBookById(user.getId(), System.nanoTime(), BookUpdateDTO.builder().build()));
    }

    @Rollback
    @Transactional
    @Test
    void updateUserBookByIdBadRequest() throws Exception {
        final String bookName = "Book 012345678901234567890123456789";

        Book book = bookRepository.findAll().getFirst();

        BookUpdateDTO bookUpdateDTO = BookUpdateDTO.builder()
                .name(bookName)
                .description(book.getDescription())
                .author(book.getAuthor())
                .category(book.getCategory())
                .publicationYear(book.getPublicationYear())
                .photoBase64(book.getPhotoBase64())
                .city(book.getCity())
                .contactDetails(book.getContactDetails())
                .isGift(book.getIsGift())
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(BookController.BOOK_PATH_USER_BOOK_ID, user, bookId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookUpdateDTO))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()", is(1)))
                .andReturn();

        System.out.println(mvcResult.getResponse().getContentAsString());
    }
}