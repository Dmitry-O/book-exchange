package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.BookAdminDTO;
import com.example.bookexchange.admin.mapper.AdminMapper;
import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.mapper.BookMapper;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.book.search.BookSearchIndexService;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.SoftDeleteFilterHelper;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.notification.NotificationDispatchService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_BOOK_SEARCH_DISABLED;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_BOOK_SEARCH_REINDEXED;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_BOOK_DELETED;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_BOOK_PHOTO_DELETED;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_BOOK_RESTORED;
import static com.example.bookexchange.common.i18n.MessageKey.BOOK_CANT_BE_EDITED_DURING_EXCHANGE;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_BOOK_UPDATED;
import static com.example.bookexchange.common.result.ResultFactory.error;
import static com.example.bookexchange.common.result.ResultFactory.ok;
import static com.example.bookexchange.common.result.ResultFactory.okMessage;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private SoftDeleteFilterHelper softDeleteFilterHelper;

    @Mock
    private VersionedEntityTransitionHelper versionedEntityTransitionHelper;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private BookSearchIndexService bookSearchIndexService;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @Mock
    private ExchangeRepository exchangeRepository;

    @InjectMocks
    private AdminBookServiceImpl adminBookService;

    @Test
    void shouldKeepDeletedBooksAtEnd_whenAdminListsAllBooksWithoutExplicitSort() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(softDeleteFilterHelper.runWithoutDeletedFilter(any())).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(java.util.List.of()));

        Result<org.springframework.data.domain.Page<BookAdminDTO>> result = adminBookService.findBooks(
                BookSearchDTO.builder().build(),
                UnitTestDataFactory.pageQuery(0, 20),
                BookType.ALL
        );

        assertSuccess(result, HttpStatus.OK);
        verify(bookRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().toList()).extracting(order -> order.getProperty())
                .containsExactly("deletedAt", "createdAt", "id");
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("deletedAt").getDirection().name()).isEqualTo("ASC");
    }

    @Test
    void shouldApplyChanges_whenAdminUpdatesBookWithCurrentVersion() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);
        BookUpdateDTO dto = UnitTestDataFactory.bookUpdateDto();
        BookAdminDTO adminDto = mock(BookAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(exchangeRepository.existsByStatusInAndBookId(
                Set.of(ExchangeStatus.PENDING, ExchangeStatus.APPROVED),
                book.getId()
        )).thenReturn(false);
        when(bookRepository.save(book)).thenReturn(book);
        when(adminMapper.bookToBookAdminDto(book)).thenReturn(adminDto);
        when(imageStorageService.replaceBookImage(user.getId(), book.getId(), dto.getPhotoBase64()))
                .thenReturn(ok("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + book.getId() + "_test.jpg"));

        Result<BookAdminDTO> result = adminBookService.updateBookById(admin, book.getId(), dto, book.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_UPDATED);
        verify(bookMapper).updateBookDtoToBook(dto, book);
        verify(bookRepository).save(book);
        assertThat(book.getPhotoUrl()).isNotBlank();
        verify(notificationDispatchService).sendAdminBookUpdatedNotification(book, admin.getUsername());
    }

    @Test
    void shouldKeepExistingPhoto_whenAdminUpdatesBookWithBlankPhotoBase64() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);
        book.setPhotoUrl("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + book.getId() + "_existing.jpg");

        BookUpdateDTO dto = UnitTestDataFactory.bookUpdateDto();
        dto.setPhotoBase64(" ");

        BookAdminDTO adminDto = mock(BookAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(exchangeRepository.existsByStatusInAndBookId(
                Set.of(ExchangeStatus.PENDING, ExchangeStatus.APPROVED),
                book.getId()
        )).thenReturn(false);
        when(bookRepository.save(book)).thenReturn(book);
        when(adminMapper.bookToBookAdminDto(book)).thenReturn(adminDto);

        Result<BookAdminDTO> result = adminBookService.updateBookById(admin, book.getId(), dto, book.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_UPDATED);
        assertThat(book.getPhotoUrl()).isEqualTo("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + book.getId() + "_existing.jpg");
        verify(imageStorageService, never()).replaceBookImage(any(), any(), any());
        verify(notificationDispatchService).sendAdminBookUpdatedNotification(book, admin.getUsername());
    }

    @Test
    void shouldRejectBookUpdate_whenBookParticipatesInExchange() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Locked book", user);
        BookUpdateDTO dto = UnitTestDataFactory.bookUpdateDto();
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(exchangeRepository.existsByStatusInAndBookId(
                Set.of(ExchangeStatus.PENDING, ExchangeStatus.APPROVED),
                book.getId()
        )).thenReturn(true);

        Result<BookAdminDTO> result = adminBookService.updateBookById(admin, book.getId(), dto, book.getVersion());

        assertFailure(result, BOOK_CANT_BE_EDITED_DURING_EXCHANGE, HttpStatus.BAD_REQUEST);
        verify(bookMapper, never()).updateBookDtoToBook(any(), any());
        verify(bookRepository, never()).save(any());
        verify(notificationDispatchService, never()).sendAdminBookUpdatedNotification(any(), any());
    }

    @Test
    void shouldAllowAdminBookUpdate_whenOnlyDeclinedExchangeExists() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Unlocked book", user);
        BookUpdateDTO dto = UnitTestDataFactory.bookUpdateDto();
        BookAdminDTO adminDto = mock(BookAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(exchangeRepository.existsByStatusInAndBookId(
                Set.of(ExchangeStatus.PENDING, ExchangeStatus.APPROVED),
                book.getId()
        )).thenReturn(false);
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(bookRepository.save(book)).thenReturn(book);
        when(adminMapper.bookToBookAdminDto(book)).thenReturn(adminDto);
        when(imageStorageService.replaceBookImage(user.getId(), book.getId(), dto.getPhotoBase64()))
                .thenReturn(ok("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + book.getId() + "_test.jpg"));

        Result<BookAdminDTO> result = adminBookService.updateBookById(admin, book.getId(), dto, book.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_UPDATED);
        verify(bookMapper).updateBookDtoToBook(dto, book);
        verify(bookRepository).save(book);
        verify(notificationDispatchService).sendAdminBookUpdatedNotification(book, admin.getUsername());
    }

    @Test
    void shouldDeleteBookPhoto_whenAdminDeletesPhotoWithCurrentVersion() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);
        book.setPhotoUrl("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + book.getId() + "_existing.jpg");

        BookAdminDTO adminDto = mock(BookAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(softDeleteFilterHelper.runWithoutDeletedFilter(any())).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(exchangeRepository.existsByStatusInAndBookId(
                Set.of(ExchangeStatus.PENDING, ExchangeStatus.APPROVED),
                book.getId()
        )).thenReturn(false);
        when(imageStorageService.deleteBookImage(user.getId(), book.getId())).thenReturn(ok(null));
        when(bookRepository.save(book)).thenReturn(book);
        when(adminMapper.bookToBookAdminDto(book)).thenReturn(adminDto);

        Result<BookAdminDTO> result = adminBookService.deleteBookPhotoById(admin, book.getId(), book.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_PHOTO_DELETED);
        assertThat(book.getPhotoUrl()).isNull();
        verify(imageStorageService).deleteBookImage(user.getId(), book.getId());
        verify(bookRepository).save(book);
        verify(notificationDispatchService).sendAdminBookPhotoDeletedNotification(book, admin.getUsername());
    }

    @Test
    void shouldRejectPhotoDelete_whenBookParticipatesInExchange() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Locked book", user);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(softDeleteFilterHelper.runWithoutDeletedFilter(any())).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        when(exchangeRepository.existsByStatusInAndBookId(
                Set.of(ExchangeStatus.PENDING, ExchangeStatus.APPROVED),
                book.getId()
        )).thenReturn(true);

        Result<BookAdminDTO> result = adminBookService.deleteBookPhotoById(admin, book.getId(), book.getVersion());

        assertFailure(result, BOOK_CANT_BE_EDITED_DURING_EXCHANGE, HttpStatus.BAD_REQUEST);
        verify(imageStorageService, never()).deleteBookImage(any(), any());
        verify(bookRepository, never()).save(any());
        verify(notificationDispatchService, never()).sendAdminBookPhotoDeletedNotification(any(), any());
    }

    @Test
    void shouldAllowAdminBookPhotoDelete_whenOnlyDeclinedExchangeExists() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);
        book.setPhotoUrl("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + book.getId() + "_existing.jpg");
        BookAdminDTO adminDto = mock(BookAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(softDeleteFilterHelper.runWithoutDeletedFilter(any())).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        when(exchangeRepository.existsByStatusInAndBookId(
                Set.of(ExchangeStatus.PENDING, ExchangeStatus.APPROVED),
                book.getId()
        )).thenReturn(false);
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(imageStorageService.deleteBookImage(user.getId(), book.getId())).thenReturn(ok(null));
        when(bookRepository.save(book)).thenReturn(book);
        when(adminMapper.bookToBookAdminDto(book)).thenReturn(adminDto);

        Result<BookAdminDTO> result = adminBookService.deleteBookPhotoById(admin, book.getId(), book.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_PHOTO_DELETED);
        verify(imageStorageService).deleteBookImage(user.getId(), book.getId());
        verify(bookRepository).save(book);
        verify(notificationDispatchService).sendAdminBookPhotoDeletedNotification(book, admin.getUsername());
    }

    @Test
    void shouldSetDeletedAt_whenAdminDeletesBookWithCurrentVersion() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);
        BookAdminDTO adminDto = mock(BookAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(exchangeRepository.findByStatusAndBookId(ExchangeStatus.PENDING, book.getId())).thenReturn(java.util.List.of());
        when(adminMapper.bookToBookAdminDto(book)).thenReturn(adminDto);

        Result<BookAdminDTO> result = adminBookService.deleteBookById(admin, book.getId(), book.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_DELETED);
        assertThat(book.getDeletedAt()).isNotNull();
        verify(notificationDispatchService).sendAdminBookDeletedNotification(book, admin.getUsername());
    }

    @Test
    void shouldDeclinePendingExchanges_whenAdminDeletesBook() {
        User owner = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        User otherUser = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", owner);
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", otherUser);
        Exchange pendingExchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                owner,
                otherUser,
                book,
                receiverBook,
                ExchangeStatus.PENDING
        );
        pendingExchange.setIsReadBySender(true);
        pendingExchange.setIsReadByReceiver(true);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(exchangeRepository.findByStatusAndBookId(ExchangeStatus.PENDING, book.getId()))
                .thenReturn(java.util.List.of(pendingExchange));

        Result<BookAdminDTO> result = adminBookService.deleteBookById(admin, book.getId(), book.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_DELETED);
        assertThat(pendingExchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(pendingExchange.getDeclinerUser()).isNull();
        assertThat(pendingExchange.getAutoDeclined()).isTrue();
        assertThat(pendingExchange.getIsReadBySender()).isFalse();
        assertThat(pendingExchange.getIsReadByReceiver()).isFalse();
        verify(exchangeRepository).save(pendingExchange);
        verify(notificationDispatchService).sendExchangeAutoDeclinedNotifications(java.util.List.of(pendingExchange));
    }

    @Test
    void shouldClearDeletedAt_whenAdminRestoresBookWithCurrentVersion() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);
        book.setDeletedAt(java.time.Instant.now());
        BookAdminDTO adminDto = mock(BookAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(softDeleteFilterHelper.runWithoutDeletedFilter(any())).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(adminMapper.bookToBookAdminDto(book)).thenReturn(adminDto);

        Result<BookAdminDTO> result = adminBookService.restoreBookById(admin, book.getId(), book.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_RESTORED);
        assertThat(book.getDeletedAt()).isNull();
        verify(notificationDispatchService).sendAdminBookRestoredNotification(book, admin.getUsername());
    }

    @Test
    void shouldReindexBookSearch_whenAdminTriggersManualReindex() {
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");
        User owner = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book firstBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "First book", owner);
        Book secondBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Second book", owner);

        when(softDeleteFilterHelper.runWithoutDeletedFilter(any())).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        when(bookRepository.findAll()).thenReturn(java.util.List.of(firstBook, secondBook));
        when(bookSearchIndexService.reindexAll(java.util.List.of(firstBook, secondBook)))
                .thenReturn(okMessage(ADMIN_BOOK_SEARCH_REINDEXED, 2));

        Result<Void> result = adminBookService.reindexSearch(admin);

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_SEARCH_REINDEXED);
        verify(bookSearchIndexService).reindexAll(java.util.List.of(firstBook, secondBook));
    }

    @Test
    void shouldReturnFailure_whenBookSearchReindexIsDisabled() {
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(softDeleteFilterHelper.runWithoutDeletedFilter(any())).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        when(bookRepository.findAll()).thenReturn(java.util.List.of());
        when(bookSearchIndexService.reindexAll(java.util.List.of()))
                .thenReturn(error(ADMIN_BOOK_SEARCH_DISABLED, HttpStatus.BAD_REQUEST));

        Result<Void> result = adminBookService.reindexSearch(admin);

        assertFailure(result, ADMIN_BOOK_SEARCH_DISABLED, HttpStatus.BAD_REQUEST);
    }
}
