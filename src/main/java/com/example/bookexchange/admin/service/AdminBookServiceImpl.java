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
import com.example.bookexchange.book.specification.BookSpecificationBuilder;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.SoftDeleteFilterHelper;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.dto.SortDirectionDTO;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.common.util.ETagUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminBookServiceImpl implements AdminBookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final AdminMapper adminMapper;
    private final AuditService auditService;
    private final SoftDeleteFilterHelper softDeleteFilterHelper;
    private final VersionedEntityTransitionHelper versionedEntityTransitionHelper;
    private final ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    @Override
    public Result<Page<BookAdminDTO>> findBooks(BookSearchDTO dto, PageQueryDTO queryDTO, BookType bookType) {
        return softDeleteFilterHelper.runWithoutDeletedFilter(() -> {
            BookSearchDTO searchDto = normalizeSearchDto(dto);
            Specification<Book> specification = BookSpecificationBuilder.build(searchDto, bookType);
            Pageable pageable = createSearchPageable(searchDto, queryDTO.getPageIndex(), queryDTO.getPageSize(), bookType);

            Page<BookAdminDTO> page = bookRepository.findAll(specification, pageable).map(adminMapper::bookToBookAdminDto);

            return ResultFactory.ok(page);
        });
    }

    private BookSearchDTO normalizeSearchDto(BookSearchDTO dto) {
        return dto != null ? dto : BookSearchDTO.builder().build();
    }

    private Pageable createSearchPageable(BookSearchDTO dto, Integer pageIndex, Integer pageSize, BookType bookType) {
        if (dto.getSortBy() == null || dto.getSortDirection() == null) {
            String defaultProperty = bookType == BookType.DELETED ? "deletedAt" : "createdAt";

            return PageRequest.of(
                    pageIndex,
                    pageSize,
                    Sort.by(Sort.Direction.DESC, defaultProperty).and(Sort.by(Sort.Direction.DESC, "id"))
            );
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

        return PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by(primaryOrder).and(Sort.by(Sort.Direction.DESC, "id"))
        );
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

                                    return ResultFactory.okETag(
                                            adminMapper.bookToBookAdminDto(book),
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
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_BOOK_UPDATE")
                                .result(AuditResult.SUCCESS)
                                .actorEmail(adminUser.getUsername())
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("bookId", bookId)
                                .detail("bookName", updatedBook.getName())
                                .build()
                        );

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

                                        auditService.log(AuditEvent.builder()
                                                .action("ADMIN_BOOK_PHOTO_DELETE")
                                                .result(AuditResult.SUCCESS)
                                                .actorEmail(adminUser.getUsername())
                                                .detail("actorUserRoles", adminUser.getAuthorities())
                                                .detail("bookId", bookId)
                                                .detail("bookName", updatedBook.getName())
                                                .build()
                                        );

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

                    book.setDeletedAt(Instant.now());

                    auditService.log(AuditEvent.builder()
                            .action("ADMIN_BOOK_DELETE")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("bookId", bookId)
                            .detail("bookName", book.getName())
                            .build()
                    );

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

                            auditService.log(AuditEvent.builder()
                                    .action("ADMIN_BOOK_RESTORE")
                                    .result(AuditResult.SUCCESS)
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("bookId", bookId)
                                    .detail("bookName", book.getName())
                                    .build()
                            );

                            return ResultFactory.updated(
                                    adminMapper.bookToBookAdminDto(book),
                                    MessageKey.ADMIN_BOOK_RESTORED,
                                    ETagUtil.form(book),
                                    book.getName()
                            );
                        })
        );
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

    private <T> Result<T> rollbackOnFailure(Result<T> result) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return result;
    }
}
