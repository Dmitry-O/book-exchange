package com.example.bookexchange.book.service;

import com.example.bookexchange.book.mapper.BookMapper;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.book.specification.BookSpecificationBuilder;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.book.dto.BookCreateDTO;
import com.example.bookexchange.book.dto.BookDTO;
import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.common.dto.SortDirectionDTO;
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
    private final VersionedEntityTransitionHelper versionedEntityTransitionHelper;

    @Transactional
    @Override
    public Result<BookDTO> addUserBook(Long userId, BookCreateDTO dto) {
        return ResultFactory
                .fromRepository(userRepository, userId, MessageKey.USER_ACCOUNT_NOT_FOUND)
                .flatMap(user -> {
                    Book book = bookMapper.bookDtoToBook(dto);
                    book.setUser(user);
                    book = bookRepository.save(book);

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
                });
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<BookDTO>> findUserBooks(Long userId, PageQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPageIndex(),
                queryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

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
                .flatMap(book ->
                        ResultFactory.okETag(
                                bookMapper.bookToBookDto(book),
                                ETagUtil.form(book)
                        )
                );
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<BookDTO>> findExchangedUserBooks(Long userId, PageQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPageIndex(),
                queryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<BookDTO> bookPage = bookRepository
                .findByUserIdAndIsExchanged(userId, true, pageable)
                .map(bookMapper::bookToBookDto);

        return ResultFactory.ok(bookPage);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<BookDTO>> findBooks(BookSearchDTO dto, PageQueryDTO queryDTO) {
        BookSearchDTO searchDto = normalizeSearchDto(dto);
        Specification<Book> specification = BookSpecificationBuilder.build(searchDto, BookType.ACTIVE);
        Pageable pageable = createSearchPageable(searchDto, queryDTO.getPageIndex(), queryDTO.getPageSize());

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

    private BookSearchDTO normalizeSearchDto(BookSearchDTO dto) {
        return dto != null ? dto : BookSearchDTO.builder().build();
    }

    private Pageable createSearchPageable(BookSearchDTO dto, Integer pageIndex, Integer pageSize) {
        if (dto.getSortBy() == null || dto.getSortDirection() == null) {
            return PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Sort.Direction direction = dto.getSortDirection() == SortDirectionDTO.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, dto.getSortBy().getProperty()));
    }

    private Result<Book> validateBookVersion(Book book, Long version, String action, Long actorId) {
        return versionedEntityTransitionHelper.requireVersion(
                book,
                version,
                action,
                builder -> builder
                        .actorId(actorId)
                        .actorEmail(book.getUser().getEmail())
                        .detail("bookId", book.getId())
                        .detail("bookName", book.getName())
        );
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

}
