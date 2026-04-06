package com.example.bookexchange.book.service;

import com.example.bookexchange.book.dto.BookCreateDTO;
import com.example.bookexchange.book.dto.BookDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.mapper.BookMapper;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.bookexchange.common.i18n.MessageKey.BOOK_CREATED;
import static com.example.bookexchange.common.i18n.MessageKey.BOOK_DELETED;
import static com.example.bookexchange.common.i18n.MessageKey.BOOK_UPDATED;
import static com.example.bookexchange.common.result.ResultFactory.ok;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    @InjectMocks
    private BookServiceImpl bookService;

    @Test
    void shouldPersistBookForUser_whenAddUserBookIsCalled() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        BookCreateDTO dto = UnitTestDataFactory.bookCreateDto();
        Book mappedBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, dto.getName(), user);
        BookDTO bookDto = org.mockito.Mockito.mock(BookDTO.class);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(bookMapper.bookDtoToBook(dto)).thenReturn(mappedBook);
        when(bookRepository.save(mappedBook)).thenReturn(mappedBook);
        when(bookMapper.bookToBookDto(mappedBook)).thenReturn(bookDto);

        Result<BookDTO> result = bookService.addUserBook(user.getId(), dto);

        assertSuccess(result, HttpStatus.CREATED, BOOK_CREATED);
        assertThat(mappedBook.getUser()).isSameAs(user);
        verify(bookRepository).save(mappedBook);
        verify(auditService).log(any());
    }

    @Test
    void shouldUpdateBook_whenUpdateUserBookVersionMatches() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);
        BookUpdateDTO dto = UnitTestDataFactory.bookUpdateDto();
        BookDTO bookDto = org.mockito.Mockito.mock(BookDTO.class);

        when(bookRepository.findByIdAndUserId(book.getId(), user.getId())).thenReturn(Optional.of(book));
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));
        when(bookMapper.bookToBookDto(book)).thenReturn(bookDto);

        Result<BookDTO> result = bookService.updateUserBookById(user.getId(), book.getId(), dto, book.getVersion());

        assertSuccess(result, HttpStatus.OK, BOOK_UPDATED);
        verify(bookMapper).updateBookDtoToBook(dto, book);
        verify(bookRepository).save(book);
    }

    @Test
    void shouldSoftDeleteBook_whenDeleteUserBookVersionMatches() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Old book", user);

        when(bookRepository.findByIdAndUserId(book.getId(), user.getId())).thenReturn(Optional.of(book));
        when(versionedEntityTransitionHelper.requireVersion(any(Book.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(book));

        Result<Void> result = bookService.deleteUserBookById(user.getId(), book.getId(), book.getVersion());

        assertSuccess(result, HttpStatus.OK, BOOK_DELETED);
        assertThat(book.getDeletedAt()).isBeforeOrEqualTo(Instant.now());
        verify(auditService).log(any());
    }

    @Test
    void shouldMarkAllActiveBooksAsDeleted_whenSoftDeleteBooksIsCalled() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book firstBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "First", user);
        Book secondBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Second", user);
        Instant deletedAt = Instant.now();

        when(bookRepository.findAllByUserIdAndDeletedAtIsNull(user.getId())).thenReturn(List.of(firstBook, secondBook));

        bookService.softDeleteBooks(user, deletedAt);

        assertThat(firstBook.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(secondBook.getDeletedAt()).isEqualTo(deletedAt);
    }
}
