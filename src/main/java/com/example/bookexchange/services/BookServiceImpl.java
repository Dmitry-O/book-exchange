package com.example.bookexchange.services;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.dto.BookSearchDTO;
import com.example.bookexchange.dto.BookUpdateDTO;
import com.example.bookexchange.mappers.BookMapper;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.specification.BookSpecificationBuilder;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
@AllArgsConstructor
public class BookServiceImpl extends BaseServiceImpl<User, Long> implements BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookMapper bookMapper;

    @Override
    public BookDTO addUserBook(Long userId, BookCreateDTO dto) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        Book book = bookMapper.bookDtoToBook(dto);
        book.setUser(user);
        Book savedBook = bookRepository.save(book);

        return bookMapper.bookToBookDto(savedBook);
    }

    @Override
    public Page<BookDTO> findUserBooks(Long userId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Book> bookPage = bookRepository.findByUserIdAndIsExchanged(userId, false, pageable);

        return bookPage.map(bookMapper::bookToBookDto);
    }

    @Override
    public Page<BookDTO> findExchangedUserBooks(Long userId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Book> bookPage = bookRepository.findByUserIdAndIsExchanged(userId, true, pageable);

        return bookPage.map(bookMapper::bookToBookDto);
    }

    @Override
    public Page<BookDTO> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize) {
        Specification<Book> specification = BookSpecificationBuilder.build(dto);

        Pageable pageable;

        if (dto.getSortBy() != null && dto.getSortDirection() != null) {
            Sort.Direction direction = dto.getSortDirection().equalsIgnoreCase(("desc"))
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, dto.getSortBy()));
        } else {
            pageable = PageRequest.of(pageIndex, pageSize);
        }

        Page<Book> bookPage = bookRepository.findAll(specification, pageable);

        return bookPage.map(bookMapper::bookToBookDto);
    }

    @Override
    public Boolean deleteUserBookById(Long userId, Long bookId) {
        if (bookRepository.existsById(bookId)) {
            bookRepository.deleteByIdAndUserId(bookId, userId);

            return true;
        }

        return false;
    }

    @Transactional
    @Override
    public Optional<BookDTO> updateUserBookById(Long userId, Long bookId, BookUpdateDTO dto) {
        AtomicReference<Optional<BookDTO>> atomicReference = new AtomicReference<>();

        bookRepository.findByIdAndUserId(bookId, userId).ifPresentOrElse(foundBook -> {
            foundBook.setName(!dto.getName().isEmpty() ? dto.getName() : foundBook.getName());
            foundBook.setDescription(!dto.getDescription().isEmpty() ? dto.getDescription() : foundBook.getDescription());
            foundBook.setAuthor(!dto.getAuthor().isEmpty() ? dto.getAuthor() : foundBook.getAuthor());
            foundBook.setCategory(!dto.getCategory().isEmpty() ? dto.getCategory() : foundBook.getCategory());
            foundBook.setPublicationYear(dto.getPublicationYear() != null ? dto.getPublicationYear() : foundBook.getPublicationYear());
            foundBook.setPhotoBase64(!dto.getPhotoBase64().isEmpty() ? dto.getPhotoBase64() : foundBook.getPhotoBase64());
            foundBook.setCity(!dto.getCity().isEmpty() ? dto.getCity() : foundBook.getCity());
            foundBook.setIsGift(dto.getIsGift() != null ?  dto.getIsGift() : foundBook.getIsGift());
            foundBook.setContactDetails(!dto.getContactDetails().isEmpty() ? dto.getContactDetails() : foundBook.getContactDetails());

            atomicReference.set(
                    Optional.of(
                            bookMapper.bookToBookDto(
                                    bookRepository.save(foundBook)
                            )
                    )
            );
        }, () -> {
            atomicReference.set(Optional.empty());
        });

        return atomicReference.get();
    }
}
