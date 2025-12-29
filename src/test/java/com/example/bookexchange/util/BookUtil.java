package com.example.bookexchange.util;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.services.BookService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class BookUtil {

    private final BookService bookService;
    private final BookRepository bookRepository;

    private BookCreateDTO generateBookCreateDTO(Integer bookNumber) {
        return BookCreateDTO.builder()
                .name("Book " + bookNumber)
                .description("Book " + bookNumber)
                .author("Author " + bookNumber)
                .category("Category " + bookNumber)
                .publicationYear(2000)
                .photoBase64("photo.jpg")
                .city("City " + bookNumber)
                .contactDetails("Contact Details " + bookNumber)
                .isGift(false)
                .isExchanged(false)
                .build();
    }

    public Long createBook(Long userId, Integer bookNumberParam) {
        Integer bookNumber = Optional.ofNullable(bookNumberParam).orElse(1);

        BookDTO savedBook = bookService.addUserBook(userId, generateBookCreateDTO(bookNumber));

        return savedBook.getId();
    }

    public List<BookDTO> createSeveralBooks(Long userId, Integer numberOfSameBooks, Integer numberOfDifferentBooks) {
        List<BookDTO> bookDTOList = new ArrayList<>();

        for (Integer bookNumber = 0; bookNumber < numberOfSameBooks; bookNumber++) {
            bookDTOList.add(bookService.addUserBook(userId, generateBookCreateDTO(1)));
        }

        for (Integer bookNumber = 2; bookNumber < (2 + numberOfDifferentBooks); bookNumber++) {
            bookDTOList.add(bookService.addUserBook(userId, generateBookCreateDTO(bookNumber)));
        }

        return bookDTOList;
    }

    public void deleteUserBooks(Long userId) {
        bookRepository.deleteByUserId(userId);
    }
}
