package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.BookAdminDTO;
import com.example.bookexchange.admin.mapper.AdminMapper;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.mapper.BookMapper;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.SoftDeleteFilterHelper;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.function.Supplier;

import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_BOOK_DELETED;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_BOOK_PHOTO_DELETED;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_BOOK_RESTORED;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_BOOK_UPDATED;
import static com.example.bookexchange.common.result.ResultFactory.ok;
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

    @InjectMocks
    private AdminBookServiceImpl adminBookService;

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
        when(bookRepository.save(book)).thenReturn(book);
        when(adminMapper.bookToBookAdminDto(book)).thenReturn(adminDto);
        when(imageStorageService.replaceBookImage(user.getId(), book.getId(), dto.getPhotoBase64()))
                .thenReturn(ok("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + book.getId() + "_test.jpg"));

        Result<BookAdminDTO> result = adminBookService.updateBookById(admin, book.getId(), dto, book.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_UPDATED);
        verify(bookMapper).updateBookDtoToBook(dto, book);
        verify(bookRepository).save(book);
        assertThat(book.getPhotoUrl()).isNotBlank();
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
        when(bookRepository.save(book)).thenReturn(book);
        when(adminMapper.bookToBookAdminDto(book)).thenReturn(adminDto);

        Result<BookAdminDTO> result = adminBookService.updateBookById(admin, book.getId(), dto, book.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_UPDATED);
        assertThat(book.getPhotoUrl()).isEqualTo("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/books/" + book.getId() + "_existing.jpg");
        verify(imageStorageService, never()).replaceBookImage(any(), any(), any());
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
        when(imageStorageService.deleteBookImage(user.getId(), book.getId())).thenReturn(ok(null));
        when(bookRepository.save(book)).thenReturn(book);
        when(adminMapper.bookToBookAdminDto(book)).thenReturn(adminDto);

        Result<BookAdminDTO> result = adminBookService.deleteBookPhotoById(admin, book.getId(), book.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_PHOTO_DELETED);
        assertThat(book.getPhotoUrl()).isNull();
        verify(imageStorageService).deleteBookImage(user.getId(), book.getId());
        verify(bookRepository).save(book);
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
        when(adminMapper.bookToBookAdminDto(book)).thenReturn(adminDto);

        Result<BookAdminDTO> result = adminBookService.deleteBookById(admin, book.getId(), book.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_BOOK_DELETED);
        assertThat(book.getDeletedAt()).isNotNull();
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
    }
}
