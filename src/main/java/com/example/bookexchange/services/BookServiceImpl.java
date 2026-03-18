package com.example.bookexchange.services;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.dto.BookSearchDTO;
import com.example.bookexchange.dto.BookUpdateDTO;
import com.example.bookexchange.exception.NotFoundException;
import com.example.bookexchange.mappers.BookMapper;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.BookType;
import com.example.bookexchange.models.MessageKey;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.specification.BookSpecificationBuilder;
import com.example.bookexchange.util.Helper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@AllArgsConstructor
public class BookServiceImpl extends BaseServiceImpl<User, Long> implements BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookMapper bookMapper;
    private final Helper helper;

    private final MessageService messageService;

    @Transactional
    @Override
    public String addUserBook(Long userId, BookCreateDTO dto) {
        User user = findOrThrow(userRepository, userId, MessageKey.USER_ACCOUNT_NOT_FOUND);

        Book book = bookMapper.bookDtoToBook(dto);
        book.setUser(user);
        bookRepository.save(book);

        return messageService.getMessage(MessageKey.BOOK_CREATED);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<BookDTO> findUserBooks(Long userId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Book> bookPage = bookRepository.findByUserIdAndIsExchanged(userId, false, pageable);

        return bookPage.map(bookMapper::bookToBookDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Book findUserBookById(Long userId, Long bookId) {
        return bookRepository.findByIdAndUserId(bookId, userId).orElseThrow(() -> new NotFoundException(MessageKey.BOOK_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<BookDTO> findExchangedUserBooks(Long userId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Book> bookPage = bookRepository.findByUserIdAndIsExchanged(userId, true, pageable);

        return bookPage.map(bookMapper::bookToBookDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<BookDTO> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize) {
        Specification<Book> specification = BookSpecificationBuilder.build(dto, BookType.ACTIVE);

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

    @Transactional
    @Override
    public String deleteUserBookById(Long userId, Long bookId, Long version) {
        Book book = bookRepository.findByIdAndUserId(bookId, userId).orElseThrow(() -> new NotFoundException(MessageKey.BOOK_NOT_FOUND));

        helper.checkEntityVersion(book.getVersion(), version);

        book.setDeletedAt(Instant.now());

        return messageService.getMessage(MessageKey.BOOK_DELETED);
    }

    @Transactional
    @Override
    public String updateUserBookById(Long userId, Long bookId, BookUpdateDTO dto, Long version) {
        bookRepository.findByIdAndUserId(bookId, userId).ifPresentOrElse(foundBook -> {
            helper.checkEntityVersion(foundBook.getVersion(), version);

            bookMapper.updateBookDtoToBook(dto, foundBook);

            bookRepository.save(foundBook);
        }, () -> {
            throw new NotFoundException(MessageKey.BOOK_NOT_FOUND);
        });

        return messageService.getMessage(MessageKey.BOOK_UPDATED);
    }
}
