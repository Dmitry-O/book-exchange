package com.example.bookexchange.book.service;

import com.example.bookexchange.book.mapper.BookMapper;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.book.search.BookSearchIndexService;
import com.example.bookexchange.book.search.BookSearchResultPage;
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
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.common.util.ETagUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookMapper bookMapper;
    private final AuditService auditService;
    private final VersionedEntityTransitionHelper versionedEntityTransitionHelper;
    private final ImageStorageService imageStorageService;
    private final BookSearchIndexService bookSearchIndexService;

    @Transactional
    @Override
    public Result<BookDTO> addUserBook(Long userId, BookCreateDTO dto) {
        return ResultFactory
                .fromRepository(userRepository, userId, MessageKey.USER_ACCOUNT_NOT_FOUND)
                .flatMap(user -> {
                    Book book = bookMapper.bookDtoToBook(dto);
                    book.setUser(user);
                    Book persistedBook = bookRepository.save(book);

                    Result<Book> savedBookResult = dto.getPhotoBase64() != null && !dto.getPhotoBase64().isBlank()
                            ? imageStorageService.replaceBookImage(userId, persistedBook.getId(), dto.getPhotoBase64())
                            .flatMap(photoUrl -> {
                                persistedBook.setPhotoUrl(photoUrl);
                                return ResultFactory.ok(bookRepository.save(persistedBook));
                            })
                            : ResultFactory.ok(persistedBook);

                    if (savedBookResult.isFailure()) {
                        return rollbackOnFailure(savedBookResult.map(bookMapper::bookToBookDto));
                    }

                    return savedBookResult.flatMap(savedBook -> {
                        bookSearchIndexService.scheduleUpsert(savedBook);

                        auditService.log(AuditEvent.builder()
                                .action("BOOK_CREATE")
                                .result(AuditResult.SUCCESS)
                                .actorId(userId)
                                .detail("bookId", savedBook.getId())
                                .detail("bookName", savedBook.getName())
                                .build()
                        );

                        return ResultFactory.created(
                                bookMapper.bookToBookDto(savedBook),
                                MessageKey.BOOK_CREATED,
                                ETagUtil.form(savedBook)
                        );
                    });
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
    public Result<Page<BookDTO>> findBooks(Long currentUserId, BookSearchDTO dto, PageQueryDTO queryDTO) {
        BookSearchDTO searchDto = normalizeSearchDto(dto);
        Pageable pageable = createSearchPageable(searchDto, queryDTO.getPageIndex(), queryDTO.getPageSize());
        Optional<Result<Page<BookDTO>>> elasticsearchResult = searchBooksFromIndex(currentUserId, searchDto, queryDTO, pageable);

        if (elasticsearchResult.isPresent()) {
            return elasticsearchResult.orElseThrow();
        }

        Specification<Book> specification = BookSpecificationBuilder.build(searchDto, BookType.ACTIVE, currentUserId);

        Page<BookDTO> bookPage = bookRepository
                .findAll(specification, pageable)
                .map(bookMapper::bookToBookDto);

        return ResultFactory.ok(bookPage);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<BookDTO> findBookById(Long bookId) {
        return ResultFactory
                .fromOptional(bookRepository.findPublicBookById(bookId), MessageKey.BOOK_PUBLIC_NOT_FOUND)
                .flatMap(book -> ResultFactory.okETag(bookMapper.bookToBookDto(book), ETagUtil.form(book)));
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
                    bookSearchIndexService.scheduleUpsert(book);
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

                    Result<Book> updatedBookResult = applyPhotoChange(book, dto.getPhotoBase64(), userId, bookId);

                    if (updatedBookResult.isFailure()) {
                        return rollbackOnFailure(updatedBookResult.map(bookMapper::bookToBookDto));
                    }

                    return updatedBookResult.flatMap(updatedBook -> {
                        bookSearchIndexService.scheduleUpsert(updatedBook);
                        logBookSuccess("BOOK_UPDATE", userId, updatedBook);

                        return ResultFactory.updated(
                                bookMapper.bookToBookDto(updatedBook),
                                MessageKey.BOOK_UPDATED,
                                ETagUtil.form(updatedBook)
                        );
                    });
                });
    }

    @Transactional
    @Override
    public Result<BookDTO> deleteUserBookPhoto(Long userId, Long bookId, Long version) {
        return findUserBook(userId, bookId)
                .flatMap(book -> {
                    Result<Book> versionValidation = validateBookVersion(book, version, "BOOK_PHOTO_DELETE", userId);

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(bookMapper::bookToBookDto);
                    }

                    if (book.getPhotoUrl() == null || book.getPhotoUrl().isBlank()) {
                        return ResultFactory.updated(
                                bookMapper.bookToBookDto(book),
                                MessageKey.BOOK_PHOTO_DELETED,
                                ETagUtil.form(book)
                        );
                    }

                            return imageStorageService.deleteBookImage(userId, bookId)
                                    .flatMap(v -> {
                                        book.setPhotoUrl(null);
                                        Book updatedBook = bookRepository.save(book);
                                        bookSearchIndexService.scheduleUpsert(updatedBook);
                                        logBookSuccess("BOOK_PHOTO_DELETE", userId, updatedBook);

                                        return ResultFactory.updated(
                                        bookMapper.bookToBookDto(updatedBook),
                                        MessageKey.BOOK_PHOTO_DELETED,
                                        ETagUtil.form(updatedBook)
                                );
                            });
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
        List<Book> books = bookRepository.findAllByUserIdAndDeletedAtIsNull(user.getId());

        books.forEach(book -> {
                    book.setDeletedAt(deletedAt);
                    book.setPhotoUrl(null);
                });

        bookSearchIndexService.scheduleUpsertAll(books);
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

    private Result<Book> applyPhotoChange(Book book, String photoBase64, Long userId, Long bookId) {
        if (photoBase64 == null || photoBase64.isBlank()) {
            return ResultFactory.ok(bookRepository.save(book));
        }

        return imageStorageService.replaceBookImage(userId, bookId, photoBase64)
                .flatMap(photoUrl -> {
                    book.setPhotoUrl(photoUrl);
                    return ResultFactory.ok(bookRepository.save(book));
                });
    }

    private <T> Result<T> rollbackOnFailure(Result<T> result) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return result;
    }

    private Optional<Result<Page<BookDTO>>> searchBooksFromIndex(
            Long currentUserId,
            BookSearchDTO dto,
            PageQueryDTO queryDTO,
            Pageable pageable
    ) {
        return bookSearchIndexService.search(currentUserId, dto, queryDTO, BookType.ACTIVE)
                .flatMap(searchResult -> buildBookDtoPageFromIndex(currentUserId, searchResult, pageable)
                        .map(ResultFactory::ok));
    }

    private Optional<Page<BookDTO>> buildBookDtoPageFromIndex(
            Long currentUserId,
            BookSearchResultPage searchResult,
            Pageable pageable
    ) {
        if (searchResult.bookIds().isEmpty()) {
            return Optional.of(Page.empty(pageable));
        }

        Map<Long, Book> booksById = bookRepository.findAllByIdIn(searchResult.bookIds())
                .stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));

        if (currentUserId != null && containsCurrentUserBooks(currentUserId, booksById.values())) {
            return Optional.empty();
        }

        List<BookDTO> dtos = searchResult.bookIds().stream()
                .map(booksById::get)
                .filter(Objects::nonNull)
                .map(bookMapper::bookToBookDto)
                .toList();

        return Optional.of(new PageImpl<>(dtos, pageable, searchResult.totalHits()));
    }

    private boolean containsCurrentUserBooks(Long currentUserId, Iterable<Book> books) {
        for (Book book : books) {
            if (book != null && book.getUser() != null && Objects.equals(book.getUser().getId(), currentUserId)) {
                return true;
            }
        }

        return false;
    }

}
