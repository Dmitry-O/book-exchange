package com.example.bookexchange.support.fixture;

import com.example.bookexchange.book.dto.BookCreateDTO;
import com.example.bookexchange.book.dto.BookDTO;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.book.service.BookService;
import com.example.bookexchange.common.result.Failure;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.Success;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class BookFixtureSupport {

    private final BookService bookService;
    private final BookRepository bookRepository;

    private BookCreateDTO generateBookCreateDTO(Integer bookNumber) {
        return BookCreateDTO.builder()
                .name("Book " + bookNumber)
                .description("Book " + bookNumber)
                .author("Author " + bookNumber)
                .category("Category " + bookNumber)
                .publicationYear(2000)
                .photoBase64(validPhotoBase64(bookNumber))
                .city("City " + bookNumber)
                .contactDetails("Contact Details " + bookNumber)
                .isGift(false)
                .build();
    }

    public Long createBook(Long userId, Integer bookNumberParam) {
        Integer bookNumber = Optional.ofNullable(bookNumberParam).orElse(1);

        BookDTO savedBook = unwrap(bookService.addUserBook(userId, generateBookCreateDTO(bookNumber)));

        return savedBook.getId();
    }

    public List<BookDTO> createSeveralBooks(Long userId, Integer numberOfSameBooks, Integer numberOfDifferentBooks) {
        List<BookDTO> bookDTOList = new ArrayList<>();

        for (Integer bookNumber = 0; bookNumber < numberOfSameBooks; bookNumber++) {
            bookDTOList.add(unwrap(bookService.addUserBook(userId, generateBookCreateDTO(1))));
        }

        for (Integer bookNumber = 2; bookNumber < (2 + numberOfDifferentBooks); bookNumber++) {
            bookDTOList.add(unwrap(bookService.addUserBook(userId, generateBookCreateDTO(bookNumber))));
        }

        return bookDTOList;
    }

    public void deleteUserBooks(Long userId) {
        bookRepository.deleteByUserId(userId);
    }

    private String validPhotoBase64(Integer bookNumber) {
        return Base64.getEncoder()
                .encodeToString(("photo-" + bookNumber).getBytes(StandardCharsets.UTF_8));
    }

    private <T> T unwrap(Result<T> result) {
        if (result instanceof Success<T> success) {
            return success.body();
        }

        if (result instanceof Failure<T> failure) {
            throw new IllegalStateException(
                    "Expected success result, but got failure: "
                            + failure.messageKey()
                            + " (" + failure.status() + ")"
            );
        }

        throw new IllegalStateException("Unknown result type: " + result.getClass().getName());
    }
}
