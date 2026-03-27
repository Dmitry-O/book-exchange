package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.BookAdminDTO;
import com.example.bookexchange.admin.mapper.AdminMapper;
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
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.dto.SortDirectionDTO;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
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

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminBookServiceImpl implements AdminBookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final AdminMapper adminMapper;
    private final AuditService auditService;
    private final VersionedEntityTransitionHelper versionedEntityTransitionHelper;

    @Transactional(readOnly = true)
    @Override
    public Result<Page<BookAdminDTO>> findBooks(BookSearchDTO dto, PageQueryDTO queryDTO, BookType bookType) {
        BookSearchDTO searchDto = normalizeSearchDto(dto);
        Specification<Book> specification = BookSpecificationBuilder.build(searchDto, bookType);
        Pageable pageable = createSearchPageable(searchDto, queryDTO.getPageIndex(), queryDTO.getPageSize());

        Page<BookAdminDTO> page = bookRepository.findAll(specification, pageable).map(adminMapper::bookToBookAdminDto);

        return ResultFactory.ok(page);
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

    @Transactional(readOnly = true)
    @Override
    public Result<BookAdminDTO> findBookById(UserDetails adminUser, Long bookId) {
        return ResultFactory.fromRepository(
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
                    bookRepository.save(book);

                    auditService.log(AuditEvent.builder()
                            .action("ADMIN_BOOK_UPDATE")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("bookId", bookId)
                            .detail("bookName", book.getName())
                            .build()
                    );

                    return ResultFactory.updated(
                            adminMapper.bookToBookAdminDto(book),
                            MessageKey.ADMIN_BOOK_UPDATED,
                            ETagUtil.form(book),
                            book.getName()
                    );
                });
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
        return ResultFactory.fromRepository(
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
                });
    }
}
