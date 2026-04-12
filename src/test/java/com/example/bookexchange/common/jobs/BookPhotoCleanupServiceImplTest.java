package com.example.bookexchange.common.jobs;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.common.storage.StorageProperties;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookPhotoCleanupServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private BookPhotoCleanupServiceImpl bookPhotoCleanupService;

    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.getCleanup().setSoftDeletedBookPhotoRetentionDays(30);
        bookPhotoCleanupService = new BookPhotoCleanupServiceImpl(bookRepository, imageStorageService, storageProperties);
    }

    @Test
    void shouldDeletePhotoAndClearUrl_whenSoftDeletedBookIsOlderThanRetention() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Book cleanup", user);
        book.setDeletedAt(Instant.now().minusSeconds(31L * 24 * 60 * 60));

        when(bookRepository.findAllByDeletedAtBeforeAndPhotoUrlIsNotNull(any())).thenReturn(List.of(book));
        when(imageStorageService.deleteBookImage(user.getId(), book.getId())).thenReturn(ResultFactory.successVoid());

        bookPhotoCleanupService.deleteExpiredSoftDeletedBookPhotos();

        assertThat(book.getPhotoUrl()).isNull();
        verify(imageStorageService).deleteBookImage(user.getId(), book.getId());
    }

    @Test
    void shouldKeepPhotoUrl_whenStorageDeletionFails() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "owner@example.com", "book_owner");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Book cleanup", user);
        book.setDeletedAt(Instant.now().minusSeconds(31L * 24 * 60 * 60));
        String originalPhotoUrl = book.getPhotoUrl();

        when(bookRepository.findAllByDeletedAtBeforeAndPhotoUrlIsNotNull(any())).thenReturn(List.of(book));
        when(imageStorageService.deleteBookImage(user.getId(), book.getId()))
                .thenReturn(ResultFactory.error(MessageKey.SYSTEM_IMAGE_STORAGE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));

        bookPhotoCleanupService.deleteExpiredSoftDeletedBookPhotos();

        assertThat(book.getPhotoUrl()).isEqualTo(originalPhotoUrl);
        verify(imageStorageService).deleteBookImage(user.getId(), book.getId());
    }
}
