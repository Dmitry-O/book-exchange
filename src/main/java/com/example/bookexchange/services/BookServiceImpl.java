package com.example.bookexchange.services;

import com.example.bookexchange.core.result.*;
import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.dto.BookSearchDTO;
import com.example.bookexchange.dto.BookUpdateDTO;
import com.example.bookexchange.mappers.BookMapper;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.BookType;
import com.example.bookexchange.models.MessageKey;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.specification.BookSpecificationBuilder;
import com.example.bookexchange.util.ETagUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@AllArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookMapper bookMapper;

    @Transactional
    @Override
    public Result<BookDTO> addUserBook(Long userId, BookCreateDTO dto) {
        return ResultFactory
                .fromRepository(userRepository, userId, MessageKey.USER_ACCOUNT_NOT_FOUND)
                .map(user -> {
                    Book book = bookMapper.bookDtoToBook(dto);
                    book.setUser(user);
                    return bookRepository.save(book);
                })
                .map(book ->
                    ResultFactory.created(
                            bookMapper.bookToBookDto(book),
                            MessageKey.BOOK_CREATED,
                            ETagUtil.form(book)
                    )
                )
                .flatMap(r -> r);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<BookDTO>> findUserBooks(Long userId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<BookDTO> bookPage = bookRepository
                .findByUserIdAndIsExchanged(userId, false, pageable)
                .map(bookMapper::bookToBookDto);

        return ResultFactory.ok(bookPage);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<BookDTO> findUserBookById(Long userId, Long bookId) {
        return ResultFactory
                .fromOptional(bookRepository.findByIdAndUserId(bookId, userId), MessageKey.BOOK_NOT_FOUND)
                .map(book ->
                        ResultFactory.okETag(
                                bookMapper.bookToBookDto(book),
                                ETagUtil.form(book)
                        )
                )
                .flatMap(r -> r);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<BookDTO>> findExchangedUserBooks(Long userId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<BookDTO> bookPage = bookRepository
                .findByUserIdAndIsExchanged(userId, true, pageable)
                .map(bookMapper::bookToBookDto);

        return ResultFactory.ok(bookPage);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<BookDTO>> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize) {
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

        Page<BookDTO> bookPage = bookRepository
                .findAll(specification, pageable)
                .map(bookMapper::bookToBookDto);

        return ResultFactory.ok(bookPage);
    }

    @Transactional
    @Override
    public Result<Void> deleteUserBookById(Long userId, Long bookId, Long version) {
        return ResultFactory.fromOptional(
                    bookRepository.findByIdAndUserId(bookId, userId),
                    MessageKey.BOOK_NOT_FOUND
                )
                .flatMap(book -> {
                    if (!book.getVersion().equals(version)) {
                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                    }

                    book.setDeletedAt(Instant.now());

                    return ResultFactory.okMessage(MessageKey.BOOK_DELETED);
                });
    }

    @Transactional
    @Override
    public Result<BookDTO> updateUserBookById(Long userId, Long bookId, BookUpdateDTO dto, Long version) {
        return ResultFactory.fromOptional(
                        bookRepository.findByIdAndUserId(bookId, userId),
                        MessageKey.BOOK_NOT_FOUND
                )
                .flatMap(book -> {
                    if (!book.getVersion().equals(version)) {
                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                    }

                    bookMapper.updateBookDtoToBook(dto, book);
                    bookRepository.save(book);

                    return ResultFactory.updated(
                            bookMapper.bookToBookDto(book),
                            MessageKey.BOOK_UPDATED,
                            ETagUtil.form(book)
                    );
                });
    }
}
