package com.example.bookexchange.book.service;

import com.example.bookexchange.book.dto.BookCreateDTO;
import com.example.bookexchange.book.dto.BookDTO;
import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.dto.BookSortFieldDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.mapper.BookMapper;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.book.search.BookSearchIndexService;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.dto.SortDirectionDTO;
import com.example.bookexchange.common.notification.NotificationDispatchService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.example.bookexchange.common.i18n.MessageKey.BOOK_CREATED;
import static com.example.bookexchange.common.i18n.MessageKey.BOOK_CANT_BE_EDITED_DURING_EXCHANGE;
import static com.example.bookexchange.common.i18n.MessageKey.BOOK_DELETED;
import static com.example.bookexchange.common.i18n.MessageKey.BOOK_PHOTO_DELETED;
import static com.example.bookexchange.common.i18n.MessageKey.BOOK_PUBLIC_NOT_FOUND;
import static com.example.bookexchange.common.i18n.MessageKey.BOOK_UPDATED;
import static com.example.bookexchange.common.result.ResultFactory.ok;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private VersionedEntityTransitionHelper versionedEntityTransitionHelper;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private BookSearchIndexService bookSearchIndexService;

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private BookServiceImpl bookService;

    @Test
    void shouldPersistBookForUser_whenAddUserBookIsCalled() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        BookCreateDTO dto = UnitTestDataFactory.bookCreateDto();
        Book mappedBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, dto.getName(), user);
        BookDTO bookDto = mock(BookDTO.class);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(bookMapper.bookDtoToBook(dto)).thenReturn(mappedBook);
        when(bookRepository.save(mappedBook)).thenReturn(mappedBook);
        when(bookMapper.bookToBookDto(mappedBook)).thenReturn(bookDto);
        when(imageStorageService.replaceBookImage(user.getId(), mappedBook.getId(), dto.getPhotoBase64()))
                .thenReturn(ok("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + mappedBook.getId() + "_test.jpg"));

        Result<BookDTO> result = bookService.addUserBook(user.getId(), dto);

        assertSuccess(result, HttpStatus.CREATED, BOOK_CREATED);
        assertThat(mappedBook.getUser()).isSameAs(user);
        assertThat(mappedBook.getPhotoUrl()).isNotBlank();
        verify(bookRepository, times(2)).save(mappedBook);
        verify(auditService).log(any());
    }

    @Test
    void shouldUpdateBook_whenUpdateUserBookVersionMatches() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);
        BookUpdateDTO dto = UnitTestDataFactory.bookUpdateDto();
        BookDTO bookDto = mock(BookDTO.class);

        when(bookRepository.findByIdAndUserId(book.getId(), user.getId())).thenReturn(Optional.of(book));
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(bookRepository.save(book)).thenReturn(book);
        when(bookMapper.bookToBookDto(book)).thenReturn(bookDto);
        when(imageStorageService.replaceBookImage(user.getId(), book.getId(), dto.getPhotoBase64()))
                .thenReturn(ok("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + book.getId() + "_test.jpg"));

        Result<BookDTO> result = bookService.updateUserBookById(user.getId(), book.getId(), dto, book.getVersion());

        assertSuccess(result, HttpStatus.OK, BOOK_UPDATED);
        verify(bookMapper).updateBookDtoToBook(dto, book);
        verify(bookRepository).save(book);
        assertThat(book.getPhotoUrl()).isNotBlank();
    }

    @Test
    void shouldKeepExistingPhoto_whenUpdateUserBookReceivesBlankPhotoBase64() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);
        book.setPhotoUrl("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + book.getId() + "_existing.jpg");

        BookUpdateDTO dto = UnitTestDataFactory.bookUpdateDto();
        dto.setPhotoBase64(" ");

        BookDTO bookDto = mock(BookDTO.class);

        when(bookRepository.findByIdAndUserId(book.getId(), user.getId())).thenReturn(Optional.of(book));
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(bookRepository.save(book)).thenReturn(book);
        when(bookMapper.bookToBookDto(book)).thenReturn(bookDto);

        Result<BookDTO> result = bookService.updateUserBookById(user.getId(), book.getId(), dto, book.getVersion());

        assertSuccess(result, HttpStatus.OK, BOOK_UPDATED);
        assertThat(book.getPhotoUrl()).isEqualTo("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + book.getId() + "_existing.jpg");
        verify(imageStorageService, never()).replaceBookImage(any(), any(), any());
    }

    @Test
    void shouldRejectBookUpdate_whenBookParticipatesInExchange() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Locked book", user);
        BookUpdateDTO dto = UnitTestDataFactory.bookUpdateDto();

        when(bookRepository.findByIdAndUserId(book.getId(), user.getId())).thenReturn(Optional.of(book));
        when(exchangeRepository.existsByStatusInAndBookId(
                Set.of(ExchangeStatus.PENDING, ExchangeStatus.APPROVED),
                book.getId()
        )).thenReturn(true);

        Result<BookDTO> result = bookService.updateUserBookById(user.getId(), book.getId(), dto, book.getVersion());

        assertFailure(result, BOOK_CANT_BE_EDITED_DURING_EXCHANGE, HttpStatus.BAD_REQUEST);
        verify(bookMapper, never()).updateBookDtoToBook(any(), any());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void shouldAllowBookUpdate_whenOnlyDeclinedExchangeExists() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Unlocked book", user);
        BookUpdateDTO dto = UnitTestDataFactory.bookUpdateDto();
        BookDTO bookDto = mock(BookDTO.class);

        when(bookRepository.findByIdAndUserId(book.getId(), user.getId())).thenReturn(Optional.of(book));
        when(exchangeRepository.existsByStatusInAndBookId(
                Set.of(ExchangeStatus.PENDING, ExchangeStatus.APPROVED),
                book.getId()
        )).thenReturn(false);
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(bookRepository.save(book)).thenReturn(book);
        when(bookMapper.bookToBookDto(book)).thenReturn(bookDto);
        when(imageStorageService.replaceBookImage(user.getId(), book.getId(), dto.getPhotoBase64()))
                .thenReturn(ok("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + book.getId() + "_test.jpg"));

        Result<BookDTO> result = bookService.updateUserBookById(user.getId(), book.getId(), dto, book.getVersion());

        assertSuccess(result, HttpStatus.OK, BOOK_UPDATED);
        verify(bookMapper).updateBookDtoToBook(dto, book);
        verify(bookRepository).save(book);
    }

    @Test
    void shouldReturnPublicBook_whenFindBookByIdIsCalledForActiveBook() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Public book", user);
        BookDTO bookDto = mock(BookDTO.class);

        when(bookRepository.findPublicBookById(book.getId())).thenReturn(Optional.of(book));
        when(bookMapper.bookToBookDto(book)).thenReturn(bookDto);

        Result<BookDTO> result = bookService.findBookById(book.getId());

        assertSuccess(result, HttpStatus.OK);
    }

    @Test
    void shouldReturnNotFound_whenFindBookByIdDoesNotMatchPublicFilters() {
        when(bookRepository.findPublicBookById(UnitFixtureIds.SENDER_BOOK_ID)).thenReturn(Optional.empty());

        Result<BookDTO> result = bookService.findBookById(UnitFixtureIds.SENDER_BOOK_ID);

        assertFailure(result, BOOK_PUBLIC_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldSoftDeleteBook_whenDeleteUserBookVersionMatches() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);

        when(bookRepository.findByIdAndUserId(book.getId(), user.getId())).thenReturn(Optional.of(book));
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(exchangeRepository.findByStatusAndBookId(ExchangeStatus.PENDING, book.getId())).thenReturn(List.of());

        Result<Void> result = bookService.deleteUserBookById(user.getId(), book.getId(), book.getVersion());

        assertSuccess(result, HttpStatus.OK, BOOK_DELETED);
        assertThat(book.getDeletedAt()).isBeforeOrEqualTo(Instant.now());
        verify(auditService).log(any());
    }

    @Test
    void shouldCancelPendingExchanges_whenDeletingBookThatParticipatesInExchange() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        User otherUser = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Locked book", user);
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", otherUser);
        Exchange pendingExchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                user,
                otherUser,
                book,
                receiverBook,
                ExchangeStatus.PENDING
        );
        pendingExchange.setIsReadBySender(true);
        pendingExchange.setIsReadByReceiver(true);

        when(bookRepository.findByIdAndUserId(book.getId(), user.getId())).thenReturn(Optional.of(book));
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(exchangeRepository.findByStatusAndBookId(ExchangeStatus.PENDING, book.getId()))
                .thenReturn(List.of(pendingExchange));

        Result<Void> result = bookService.deleteUserBookById(user.getId(), book.getId(), book.getVersion());

        assertSuccess(result, HttpStatus.OK, BOOK_DELETED);
        assertThat(book.getDeletedAt()).isNotNull();
        assertThat(pendingExchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(pendingExchange.getDeclinerUser()).isNull();
        assertThat(pendingExchange.getAutoDeclined()).isTrue();
        assertThat(pendingExchange.getIsReadBySender()).isFalse();
        assertThat(pendingExchange.getIsReadByReceiver()).isFalse();
        verify(exchangeRepository).save(pendingExchange);
        verify(notificationDispatchService).sendExchangeAutoDeclinedNotifications(List.of(pendingExchange));
    }

    @Test
    void shouldDeleteBookPhoto_whenVersionMatchesAndPhotoExists() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);
        BookDTO bookDto = mock(BookDTO.class);

        when(bookRepository.findByIdAndUserId(book.getId(), user.getId())).thenReturn(Optional.of(book));
        when(exchangeRepository.existsByStatusInAndBookId(
                Set.of(ExchangeStatus.PENDING, ExchangeStatus.APPROVED),
                book.getId()
        )).thenReturn(false);
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(imageStorageService.deleteBookImage(user.getId(), book.getId())).thenReturn(ok(null));
        when(bookRepository.save(book)).thenReturn(book);
        when(bookMapper.bookToBookDto(book)).thenReturn(bookDto);

        Result<BookDTO> result = bookService.deleteUserBookPhoto(user.getId(), book.getId(), book.getVersion());

        assertSuccess(result, HttpStatus.OK, BOOK_PHOTO_DELETED);
        assertThat(book.getPhotoUrl()).isNull();
        verify(imageStorageService).deleteBookImage(user.getId(), book.getId());
        verify(bookRepository).save(book);
    }

    @Test
    void shouldAllowBookPhotoDelete_whenOnlyDeclinedExchangeExists() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);
        BookDTO bookDto = mock(BookDTO.class);

        when(bookRepository.findByIdAndUserId(book.getId(), user.getId())).thenReturn(Optional.of(book));
        when(exchangeRepository.existsByStatusInAndBookId(
                Set.of(ExchangeStatus.PENDING, ExchangeStatus.APPROVED),
                book.getId()
        )).thenReturn(false);
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(imageStorageService.deleteBookImage(user.getId(), book.getId())).thenReturn(ok(null));
        when(bookRepository.save(book)).thenReturn(book);
        when(bookMapper.bookToBookDto(book)).thenReturn(bookDto);

        Result<BookDTO> result = bookService.deleteUserBookPhoto(user.getId(), book.getId(), book.getVersion());

        assertSuccess(result, HttpStatus.OK, BOOK_PHOTO_DELETED);
        verify(imageStorageService).deleteBookImage(user.getId(), book.getId());
        verify(bookRepository).save(book);
    }

    @Test
    void shouldMarkAllActiveBooksAsDeleted_whenSoftDeleteBooksIsCalled() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book firstBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "First", user);
        Book secondBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Second", user);
        Instant deletedAt = Instant.now();

        when(bookRepository.findAllByUserIdAndDeletedAtIsNull(user.getId())).thenReturn(List.of(firstBook, secondBook));
        when(exchangeRepository.findByStatusAndBookId(ExchangeStatus.PENDING, firstBook.getId())).thenReturn(List.of());
        when(exchangeRepository.findByStatusAndBookId(ExchangeStatus.PENDING, secondBook.getId())).thenReturn(List.of());

        bookService.softDeleteBooks(user, deletedAt);

        assertThat(firstBook.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(secondBook.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(firstBook.getPhotoUrl()).isNotNull();
        assertThat(secondBook.getPhotoUrl()).isNotNull();
    }

    @Test
    void shouldAddIdTieBreakerToPublicSearchSort_whenCustomSortIsRequested() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        BookSearchDTO searchDTO = BookSearchDTO.builder()
                .sortBy(BookSortFieldDTO.CATEGORY)
                .sortDirection(SortDirectionDTO.ASC)
                .build();
        PageQueryDTO queryDTO = UnitTestDataFactory.pageQuery(0, 20);

        when(bookSearchIndexService.search(UnitFixtureIds.VERIFIED_USER_ID, searchDTO, queryDTO, BookType.ACTIVE))
                .thenReturn(Optional.empty());
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Result<org.springframework.data.domain.Page<BookDTO>> result = bookService.findBooks(
                UnitFixtureIds.VERIFIED_USER_ID,
                searchDTO,
                queryDTO
        );

        assertSuccess(result, HttpStatus.OK);
        verify(bookRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().toList()).extracting(order -> order.getProperty())
                .containsExactly("category", "id");
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").getDirection().name()).isEqualTo("DESC");
        verifyNoInteractions(bookMapper);
    }
}
