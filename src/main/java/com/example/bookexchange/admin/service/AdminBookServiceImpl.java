package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.BookAdminDTO;
import com.example.bookexchange.admin.mapper.AdminMapper;
import com.example.bookexchange.book.dto.BookSortFieldDTO;
import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.mapper.BookMapper;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.book.search.BookSearchIndexService;
import com.example.bookexchange.book.search.BookSearchResultPage;
import com.example.bookexchange.book.specification.BookSpecificationBuilder;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.SoftDeleteFilterHelper;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.notification.NotificationDispatchService;
import com.example.bookexchange.common.result.Failure;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.dto.SortDirectionDTO;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.common.util.ETagUtil;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBookServiceImpl implements AdminBookService {

    private static final Set<ExchangeStatus> BOOK_EDIT_LOCK_STATUSES = Set.of(
            ExchangeStatus.PENDING,
            ExchangeStatus.APPROVED
    );

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final AdminMapper adminMapper;
    private final AuditService auditService;
    private final SoftDeleteFilterHelper softDeleteFilterHelper;
    private final VersionedEntityTransitionHelper versionedEntityTransitionHelper;
    private final ImageStorageService imageStorageService;
    private final BookSearchIndexService bookSearchIndexService;
    private final NotificationDispatchService notificationDispatchService;
    private final ExchangeRepository exchangeRepository;

    @Transactional(readOnly = true)
    @Override
    public Result<Page<BookAdminDTO>> findBooks(BookSearchDTO dto, PageQueryDTO queryDTO, BookType bookType) {
        return softDeleteFilterHelper.runWithoutDeletedFilter(() -> {
            BookSearchDTO searchDto = normalizeSearchDto(dto);
            Pageable pageable = createSearchPageable(searchDto, queryDTO.getPageIndex(), queryDTO.getPageSize(), bookType);
            Optional<Result<Page<BookAdminDTO>>> elasticsearchResult = searchBooksFromIndex(searchDto, queryDTO, pageable, bookType);

            if (elasticsearchResult.isPresent()) {
                return elasticsearchResult.orElseThrow();
            }

            Specification<Book> specification = BookSpecificationBuilder.build(searchDto, bookType);

            Page<BookAdminDTO> page = bookRepository.findAll(specification, pageable).map(adminMapper::bookToBookAdminDto);

            return ResultFactory.ok(page);
        });
    }

    private BookSearchDTO normalizeSearchDto(BookSearchDTO dto) {
        return dto != null ? dto : BookSearchDTO.builder().build();
    }

    private Pageable createSearchPageable(BookSearchDTO dto, Integer pageIndex, Integer pageSize, BookType bookType) {
        if (dto.getSortBy() == null || dto.getSortDirection() == null) {
            Sort sort = bookType == BookType.ALL
                    ? Sort.by(Sort.Order.asc("deletedAt"))
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .and(Sort.by(Sort.Direction.DESC, "id"))
                    : Sort.by(Sort.Direction.DESC, bookType == BookType.DELETED ? "deletedAt" : "createdAt")
                            .and(Sort.by(Sort.Direction.DESC, "id"));

            return PageRequest.of(pageIndex, pageSize, sort);
        }

        Sort.Direction direction = dto.getSortDirection() == SortDirectionDTO.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort.Order primaryOrder = new Sort.Order(direction, dto.getSortBy().getProperty());

        if (dto.getSortBy() == BookSortFieldDTO.NAME
                || dto.getSortBy() == BookSortFieldDTO.AUTHOR
                || dto.getSortBy() == BookSortFieldDTO.CATEGORY
                || dto.getSortBy() == BookSortFieldDTO.CITY) {
            primaryOrder = primaryOrder.ignoreCase();
        }

        Sort sort = Sort.by(primaryOrder).and(Sort.by(Sort.Direction.DESC, "id"));

        if (bookType == BookType.ALL) {
            sort = Sort.by(Sort.Order.asc("deletedAt")).and(sort);
        }

        return PageRequest.of(pageIndex, pageSize, sort);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<BookAdminDTO> findBookById(UserDetails adminUser, Long bookId) {
        return softDeleteFilterHelper.runWithoutDeletedFilter(() ->
                ResultFactory.fromRepository(
                                bookRepository,
                                bookId,
                                MessageKey.ADMIN_BOOK_NOT_FOUND
                        )
                        .flatMap(book -> {
                                    auditService.log(AuditEvent.builder()
                                            .action("ADMIN_BOOK_FIND")
                                            .result(AuditResult.SUCCESS)
                                            .actorEmail(adminUser.getUsername())
                                            .detail("actorUserRoles", adminUser.getAuthorities())
                                            .detail("bookId", bookId)
                                            .detail("bookName", book.getName())
                                            .build()
                                    );

                                    BookAdminDTO dto = adminMapper.bookToBookAdminDto(book);
                                    dto.setEditLocked(isBookLockedForEditing(book));

                                    return ResultFactory.okETag(
                                            dto,
                                            ETagUtil.form(book)
                                    );
                                }

                        )
        );
    }

    @Transactional
    @Override
    public Result<BookAdminDTO> updateBookById(UserDetails adminUser, Long bookId, BookUpdateDTO dto, Long version) {
        return ResultFactory.fromRepository(
                        bookRepository,
                        bookId,
                        MessageKey.ADMIN_BOOK_NOT_FOUND
                )
                .flatMap(book -> {
                    Result<Book> exchangeValidation = validateBookNotInExchange(book, "ADMIN_BOOK_UPDATE", adminUser);

                    if (exchangeValidation.isFailure()) {
                        return exchangeValidation.map(adminMapper::bookToBookAdminDto);
                    }

                    Result<Book> versionValidation = versionedEntityTransitionHelper.requireVersion(
                            book,
                            version,
                            "ADMIN_BOOK_UPDATE",
                            builder -> builder
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("bookId", bookId)
                                    .detail("bookName", book.getName())
                    );

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(adminMapper::bookToBookAdminDto);
                    }

                    bookMapper.updateBookDtoToBook(dto, book);

                    Result<Book> updatedBookResult = applyPhotoChange(book, dto.getPhotoBase64());

                    if (updatedBookResult.isFailure()) {
                        return rollbackOnFailure(updatedBookResult.map(adminMapper::bookToBookAdminDto));
                    }

                    return updatedBookResult.flatMap(updatedBook -> {
                        bookSearchIndexService.scheduleUpsert(updatedBook);

                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_BOOK_UPDATE")
                                .result(AuditResult.SUCCESS)
                                .actorEmail(adminUser.getUsername())
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("bookId", bookId)
                                .detail("bookName", updatedBook.getName())
                                .build()
                        );

                        notificationDispatchService.sendAdminBookUpdatedNotification(updatedBook, adminUser.getUsername());

                        return ResultFactory.updated(
                                adminMapper.bookToBookAdminDto(updatedBook),
                                MessageKey.ADMIN_BOOK_UPDATED,
                                ETagUtil.form(updatedBook),
                                updatedBook.getName()
                        );
                    });
                });
    }

    @Transactional
    @Override
    public Result<BookAdminDTO> deleteBookPhotoById(UserDetails adminUser, Long bookId, Long version) {
        return softDeleteFilterHelper.runWithoutDeletedFilter(() ->
                ResultFactory.fromRepository(
                                bookRepository,
                                bookId,
                                MessageKey.ADMIN_BOOK_NOT_FOUND
                        )
                        .flatMap(book -> {
                            Result<Book> exchangeValidation = validateBookNotInExchange(book, "ADMIN_BOOK_PHOTO_DELETE", adminUser);

                            if (exchangeValidation.isFailure()) {
                                return exchangeValidation.map(adminMapper::bookToBookAdminDto);
                            }

                            Result<Book> versionValidation = versionedEntityTransitionHelper.requireVersion(
                                    book,
                                    version,
                                    "ADMIN_BOOK_PHOTO_DELETE",
                                    builder -> builder
                                            .actorEmail(adminUser.getUsername())
                                            .detail("actorUserRoles", adminUser.getAuthorities())
                                            .detail("bookId", bookId)
                                            .detail("bookName", book.getName())
                            );

                            if (versionValidation.isFailure()) {
                                return versionValidation.map(adminMapper::bookToBookAdminDto);
                            }

                            if (book.getPhotoUrl() == null || book.getPhotoUrl().isBlank()) {
                                return ResultFactory.updated(
                                        adminMapper.bookToBookAdminDto(book),
                                        MessageKey.ADMIN_BOOK_PHOTO_DELETED,
                                        ETagUtil.form(book),
                                        book.getName()
                                );
                            }

                            return imageStorageService.deleteBookImage(book.getUser().getId(), book.getId())
                                    .flatMap(v -> {
                                        book.setPhotoUrl(null);
                                        Book updatedBook = bookRepository.save(book);
                                        bookSearchIndexService.scheduleUpsert(updatedBook);

                                        auditService.log(AuditEvent.builder()
                                                .action("ADMIN_BOOK_PHOTO_DELETE")
                                                .result(AuditResult.SUCCESS)
                                                .actorEmail(adminUser.getUsername())
                                                .detail("actorUserRoles", adminUser.getAuthorities())
                                                .detail("bookId", bookId)
                                                .detail("bookName", updatedBook.getName())
                                                .build()
                                        );

                                        notificationDispatchService.sendAdminBookPhotoDeletedNotification(updatedBook, adminUser.getUsername());

                                        return ResultFactory.updated(
                                                adminMapper.bookToBookAdminDto(updatedBook),
                                                MessageKey.ADMIN_BOOK_PHOTO_DELETED,
                                                ETagUtil.form(updatedBook),
                                                updatedBook.getName()
                                        );
                                    });
                        })
        );
    }

    @Transactional
    @Override
    public Result<BookAdminDTO> deleteBookById(UserDetails adminUser, Long bookId, Long version) {
        return ResultFactory.fromRepository(
                        bookRepository,
                        bookId,
                        MessageKey.ADMIN_BOOK_NOT_FOUND
                )
                .flatMap(book -> {
                    Result<Book> versionValidation = versionedEntityTransitionHelper.requireVersion(
                            book,
                            version,
                            "ADMIN_BOOK_DELETE",
                            builder -> builder
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("bookId", bookId)
                                    .detail("bookName", book.getName())
                    );

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(adminMapper::bookToBookAdminDto);
                    }

                    cancelPendingBookExchanges(book, book.getUser());
                    book.setDeletedAt(Instant.now());
                    bookSearchIndexService.scheduleUpsert(book);

                    auditService.log(AuditEvent.builder()
                            .action("ADMIN_BOOK_DELETE")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("bookId", bookId)
                            .detail("bookName", book.getName())
                            .build()
                    );

                    notificationDispatchService.sendAdminBookDeletedNotification(book, adminUser.getUsername());

                    return ResultFactory.updated(
                            adminMapper.bookToBookAdminDto(book),
                            MessageKey.ADMIN_BOOK_DELETED,
                            ETagUtil.form(book),
                            book.getName()
                    );
                });
    }

    @Transactional
    @Override
    public Result<BookAdminDTO> restoreBookById(UserDetails adminUser, Long bookId, Long version) {
        return softDeleteFilterHelper.runWithoutDeletedFilter(() ->
                ResultFactory.fromRepository(
                                bookRepository,
                                bookId,
                                MessageKey.ADMIN_BOOK_NOT_FOUND
                        )
                        .flatMap(book -> {
                            Result<Book> versionValidation = versionedEntityTransitionHelper.requireVersion(
                                    book,
                                    version,
                                    "ADMIN_BOOK_RESTORE",
                                    builder -> builder
                                            .actorEmail(adminUser.getUsername())
                                            .detail("actorUserRoles", adminUser.getAuthorities())
                                            .detail("bookId", bookId)
                                            .detail("bookName", book.getName())
                            );

                            if (versionValidation.isFailure()) {
                                return versionValidation.map(adminMapper::bookToBookAdminDto);
                            }

                            book.setDeletedAt(null);
                            bookSearchIndexService.scheduleUpsert(book);

                            auditService.log(AuditEvent.builder()
                                    .action("ADMIN_BOOK_RESTORE")
                                    .result(AuditResult.SUCCESS)
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("bookId", bookId)
                                    .detail("bookName", book.getName())
                                    .build()
                            );

                            notificationDispatchService.sendAdminBookRestoredNotification(book, adminUser.getUsername());

                            return ResultFactory.updated(
                                    adminMapper.bookToBookAdminDto(book),
                                    MessageKey.ADMIN_BOOK_RESTORED,
                                    ETagUtil.form(book),
                                    book.getName()
                            );
                        })
        );
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Void> reindexSearch(UserDetails adminUser) {
        return softDeleteFilterHelper.runWithoutDeletedFilter(() -> {
            List<Book> books = bookRepository.findAll();
            Result<Void> result = bookSearchIndexService.reindexAll(books);

            if (result.isSuccess()) {
                auditService.log(AuditEvent.builder()
                        .action("ADMIN_BOOK_SEARCH_REINDEX")
                        .result(AuditResult.SUCCESS)
                        .actorEmail(adminUser.getUsername())
                        .detail("actorUserRoles", adminUser.getAuthorities())
                        .detail("bookCount", books.size())
                        .build()
                );
            } else if (result instanceof Failure<Void> failure) {
                auditService.log(AuditEvent.builder()
                        .action("ADMIN_BOOK_SEARCH_REINDEX")
                        .result(AuditResult.FAILURE)
                        .actorEmail(adminUser.getUsername())
                        .detail("actorUserRoles", adminUser.getAuthorities())
                        .detail("bookCount", books.size())
                        .reason(failure.messageKey().name())
                        .build()
                );
            }

            return result;
        });
    }

    private Result<Book> applyPhotoChange(Book book, String photoBase64) {
        if (photoBase64 == null || photoBase64.isBlank()) {
            return ResultFactory.ok(bookRepository.save(book));
        }

        return imageStorageService.replaceBookImage(book.getUser().getId(), book.getId(), photoBase64)
                .flatMap(photoUrl -> {
                    book.setPhotoUrl(photoUrl);
                    return ResultFactory.ok(bookRepository.save(book));
                });
    }

    private Result<Book> validateBookNotInExchange(Book book, String action, UserDetails adminUser) {
        if (!isBookLockedForEditing(book)) {
            return ResultFactory.ok(book);
        }

        auditService.log(AuditEvent.builder()
                .action(action)
                .result(AuditResult.FAILURE)
                .actorEmail(adminUser.getUsername())
                .reason("BOOK_CANT_BE_EDITED_DURING_EXCHANGE")
                .detail("actorUserRoles", adminUser.getAuthorities())
                .detail("bookId", book.getId())
                .detail("bookName", book.getName())
                .build());

        return ResultFactory.error(MessageKey.BOOK_CANT_BE_EDITED_DURING_EXCHANGE, HttpStatus.BAD_REQUEST);
    }

    private void cancelPendingBookExchanges(Book book, com.example.bookexchange.user.model.User declinerUser) {
        List<com.example.bookexchange.exchange.model.Exchange> pendingExchanges =
                exchangeRepository.findByStatusAndBookId(com.example.bookexchange.exchange.model.ExchangeStatus.PENDING, book.getId());

        if (pendingExchanges.isEmpty()) {
            return;
        }

        pendingExchanges.forEach(exchange -> {
            exchange.setStatus(com.example.bookexchange.exchange.model.ExchangeStatus.DECLINED);
            exchange.setDeclinerUser(null);
            exchange.setAutoDeclined(Boolean.TRUE);
            com.example.bookexchange.exchange.util.ExchangeReadStateUtil.markUpdatedForBoth(exchange);
            exchangeRepository.save(exchange);
        });

        notificationDispatchService.sendExchangeAutoDeclinedNotifications(pendingExchanges);
    }

    private boolean isBookLockedForEditing(Book book) {
        return exchangeRepository.existsByStatusInAndBookId(BOOK_EDIT_LOCK_STATUSES, book.getId());
    }

    private <T> Result<T> rollbackOnFailure(Result<T> result) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return result;
    }

    private Optional<Result<Page<BookAdminDTO>>> searchBooksFromIndex(
            BookSearchDTO dto,
            PageQueryDTO queryDTO,
            Pageable pageable,
            BookType bookType
    ) {
        if (bookType == BookType.ALL) {
            return Optional.empty();
        }

        return bookSearchIndexService.search(null, dto, queryDTO, bookType)
                .map(searchResult -> ResultFactory.ok(buildBookAdminDtoPage(searchResult, pageable)));
    }

    private Page<BookAdminDTO> buildBookAdminDtoPage(BookSearchResultPage searchResult, Pageable pageable) {
        if (searchResult.bookIds().isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, Book> booksById = bookRepository.findAllByIdIn(searchResult.bookIds())
                .stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));

        List<BookAdminDTO> dtos = searchResult.bookIds().stream()
                .map(booksById::get)
                .filter(Objects::nonNull)
                .map(adminMapper::bookToBookAdminDto)
                .toList();

        return new PageImpl<>(dtos, pageable, searchResult.totalHits());
    }
}
