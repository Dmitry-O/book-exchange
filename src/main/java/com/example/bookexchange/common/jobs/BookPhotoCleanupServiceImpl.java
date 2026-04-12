package com.example.bookexchange.common.jobs;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.result.Failure;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.common.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookPhotoCleanupServiceImpl implements BookPhotoCleanupService {

    private final BookRepository bookRepository;
    private final ImageStorageService imageStorageService;
    private final StorageProperties storageProperties;

    @Transactional
    @Scheduled(cron = "${app.storage.cleanup.soft-deleted-book-photo-cron:0 30 3 * * *}")
    @Override
    public void deleteExpiredSoftDeletedBookPhotos() {
        Instant deletedBefore = Instant.now()
                .minus(storageProperties.getCleanup().getSoftDeletedBookPhotoRetentionDays(), ChronoUnit.DAYS);

        List<Book> books = bookRepository.findAllByDeletedAtBeforeAndPhotoUrlIsNotNull(deletedBefore);

        log.info("Cleaning photos for {} soft-deleted books older than {}", books.size(), deletedBefore);

        for (Book book : books) {
            Result<Void> deleteResult = imageStorageService.deleteBookImage(book.getUser().getId(), book.getId());

            if (deleteResult.isSuccess()) {
                book.setPhotoUrl(null);
                continue;
            }

            Failure<Void> failure = (Failure<Void>) deleteResult;
            log.warn(
                    "Failed to delete photo for soft-deleted book {}: {}",
                    book.getId(),
                    failure.messageKey()
            );
        }
    }
}
