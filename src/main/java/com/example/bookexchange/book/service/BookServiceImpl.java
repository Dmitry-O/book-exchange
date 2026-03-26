package com.example.bookexchange.book.service;

import com.example.bookexchange.book.mapper.BookMapper;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.book.specification.BookSpecificationBuilder;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.book.dto.BookCreateDTO;
import com.example.bookexchange.book.dto.BookDTO;
import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.common.util.ETagUtil;
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
import java.util.HashSet;

@Service
@AllArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookMapper bookMapper;
    private final AuditService auditService;

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
                .map(book -> {
                    auditService.log(AuditEvent.builder()
                            .action("BOOK_CREATE")
                            .result(AuditResult.SUCCESS)
                            .actorId(userId)
                            .detail("bookId", book.getId())
                            .detail("bookName", book.getName())
                            .build()
                    );

                    return ResultFactory.created(
                            bookMapper.bookToBookDto(book),
                            MessageKey.BOOK_CREATED,
                            ETagUtil.form(book)
                    );
                })
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
        Pageable pageable = createSearchPageable(dto, pageIndex, pageSize);

        Page<BookDTO> bookPage = bookRepository
                .findAll(specification, pageable)
                .map(bookMapper::bookToBookDto);

        return ResultFactory.ok(bookPage);
    }

    @Transactional
    @Override
    public Result<Void> deleteUserBookById(Long userId, Long bookId, Long version) {
        return findUserBook(userId, bookId)
                .flatMap(book -> {
                    Result<Book> versionValidation = validateBookVersion(book, version, "BOOK_DELETE", userId);
                    if (versionValidation.isFailure()) {
                        return versionValidation.map(v -> null);
                    }
                    book.setDeletedAt(Instant.now());
                    logBookSuccess("BOOK_DELETE", userId, book);

                    return ResultFactory.okMessage(MessageKey.BOOK_DELETED);
                });
    }

    @Transactional
    @Override
    public Result<BookDTO> updateUserBookById(Long userId, Long bookId, BookUpdateDTO dto, Long version) {
        return findUserBook(userId, bookId)
                .flatMap(book -> {
                    Result<Book> versionValidation = validateBookVersion(book, version, "BOOK_UPDATE", userId);
                    if (versionValidation.isFailure()) {
                        return versionValidation.map(bookMapper::bookToBookDto);
                    }
                    bookMapper.updateBookDtoToBook(dto, book);
                    bookRepository.save(book);
                    logBookSuccess("BOOK_UPDATE", userId, book);

                    return ResultFactory.updated(
                            bookMapper.bookToBookDto(book),
                            MessageKey.BOOK_UPDATED,
                            ETagUtil.form(book)
                    );
                });
    }

    private Result<Book> findUserBook(Long userId, Long bookId) {
        return ResultFactory.fromOptional(
                bookRepository.findByIdAndUserId(bookId, userId),
                MessageKey.BOOK_NOT_FOUND
        );
    }

    private Pageable createSearchPageable(BookSearchDTO dto, Integer pageIndex, Integer pageSize) {
        if (dto.getSortBy() == null || dto.getSortDirection() == null) {
            return PageRequest.of(pageIndex, pageSize);
        }

        Sort.Direction direction = dto.getSortDirection().equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, dto.getSortBy()));
    }

    private Result<Book> validateBookVersion(Book book, Long version, String action, Long actorId) {
        if (!book.getVersion().equals(version)) {
            logBookFailure(action, actorId, book, "SYSTEM_OPTIMISTIC_LOCK");

            return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
        }

        return ResultFactory.ok(book);
    }

    @Transactional
    @Override
    public void softDeleteBooks(User user, Instant deletedAt) {
        for (Book book : new HashSet<>(user.getBooks())) {
            if (book.getDeletedAt() == null) {
                book.setDeletedAt(deletedAt);
            }
        }
    }

    private void logBookSuccess(String action, Long actorId, Book book) {
        auditService.log(AuditEvent.builder()
                .action(action)
                .result(AuditResult.SUCCESS)
                .actorId(actorId)
                .actorEmail(book.getUser().getEmail())
                .detail("bookId", book.getId())
                .detail("bookName", book.getName())
                .build());
    }

    private void logBookFailure(String action, Long actorId, Book book, String reason) {
        auditService.log(AuditEvent.builder()
                .action(action)
                .result(AuditResult.FAILURE)
                .actorId(actorId)
                .actorEmail(book.getUser().getEmail())
                .reason(reason)
                .detail("bookId", book.getId())
                .detail("bookName", book.getName())
                .build());
    }
}
