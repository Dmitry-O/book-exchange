package com.example.bookexchange.common.demoreset;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.book.search.BookSearchIndexService;
import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.demoemail.DemoEmailSandboxService;
import com.example.bookexchange.common.demoemail.MailpitClient;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.common.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoResetServiceImpl implements DemoResetService {

    private static final String DEMO_RUNTIME_ENV = "demo";
    private static final int MAILPIT_DELETE_BATCH_SIZE = 1000;
    private static final int MAILPIT_DELETE_MAX_BATCHES = 100;
    private static final List<String> MUTABLE_TABLES = List.of(
            "user_update",
            "verification_token",
            "refresh_token",
            "report",
            "exchange",
            "book",
            "user_roles",
            "app_user"
    );

    private final AppProperties appProperties;
    private final DemoMaintenanceService demoMaintenanceService;
    private final DemoSeedImporter demoSeedImporter;
    private final JdbcTemplate jdbcTemplate;
    private final ImageStorageService imageStorageService;
    private final StorageProperties storageProperties;
    private final ObjectProvider<MailpitClient> mailpitClientProvider;
    private final DemoEmailSandboxService demoEmailSandboxService;
    private final BookRepository bookRepository;
    private final BookSearchIndexService bookSearchIndexService;
    private final AtomicBoolean resetInProgress = new AtomicBoolean(false);

    @Override
    public Result<Void> resetDemoEnvironment() {
        if (!isDemoResetAllowed()) {
            log.warn(
                    "Demo reset skipped. runtimeEnv={}, resetEnabled={}",
                    appProperties.getRuntimeEnv(),
                    appProperties.getDemoReset().isEnabled()
            );
            return ResultFactory.error(MessageKey.SYSTEM_DEMO_RESET_DISABLED, HttpStatus.BAD_REQUEST);
        }

        if (!hasSeedConfigured()) {
            log.warn("Demo reset skipped because no seed S3 key is configured");
            return ResultFactory.error(MessageKey.SYSTEM_DEMO_RESET_SEED_NOT_CONFIGURED, HttpStatus.BAD_REQUEST);
        }

        String seedS3Location;

        try {
            seedS3Location = resolveSeedS3Location();
        } catch (IllegalArgumentException ex) {
            log.warn("Demo reset skipped because seed S3 key is invalid. reason={}", ex.getMessage());
            return ResultFactory.error(MessageKey.SYSTEM_INVALID_DATA, HttpStatus.BAD_REQUEST);
        }

        if (!resetInProgress.compareAndSet(false, true)) {
            log.warn("Demo reset skipped because another reset is already in progress");
            return ResultFactory.error(MessageKey.SYSTEM_DEMO_RESET_IN_PROGRESS, HttpStatus.CONFLICT);
        }

        try {
            demoSeedImporter.validateSeedAvailable(seedS3Location);
            demoMaintenanceService.enable();
            clearMutableTables();
            demoSeedImporter.importSeed(seedS3Location);
            reindexSearch();
            clearRuntimeImages();
            clearMailpitInbox();

            log.info("Demo environment reset completed");
            return ResultFactory.okMessage(MessageKey.ADMIN_DEMO_RESET_COMPLETED);
        } catch (RuntimeException ex) {
            log.error("Demo environment reset failed. reason={}", ex.getMessage(), ex);
            return ResultFactory.error(MessageKey.SYSTEM_UNEXPECTED_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            demoMaintenanceService.disable();
            resetInProgress.set(false);
        }
    }

    private boolean isDemoResetAllowed() {
        return DEMO_RUNTIME_ENV.equalsIgnoreCase(appProperties.getRuntimeEnv())
                && appProperties.getDemoReset().isEnabled();
    }

    private boolean hasSeedConfigured() {
        return hasText(appProperties.getDemoReset().getSeedS3Key());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveSeedS3Location() {
        return "s3://" + resolveStorageBucketName() + "/" + normalizeS3Key(appProperties.getDemoReset().getSeedS3Key());
    }

    private String resolveStorageBucketName() {
        return storageProperties.getS3().isUseTestBucket()
                ? storageProperties.getS3().getTestBucket()
                : storageProperties.getS3().getProdBucket();
    }

    private String normalizeS3Key(String key) {
        String normalizedKey = key.trim();

        while (normalizedKey.startsWith("/")) {
            normalizedKey = normalizedKey.substring(1);
        }

        if (normalizedKey.isBlank()) {
            throw new IllegalArgumentException("Demo seed S3 key must not be blank");
        }

        return normalizedKey;
    }

    private void clearMutableTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");

        try {
            for (String table : MUTABLE_TABLES) {
                jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`");
            }
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private void reindexSearch() {
        if (!appProperties.getDemoReset().isReindexSearch()) {
            return;
        }

        List<Book> books = bookRepository.findAllForSearchIndex();
        Result<Void> result = bookSearchIndexService.reindexAll(books);

        if (result.isFailure()) {
            log.warn("Demo reset completed DB seed but search reindex failed. bookCount={}", books.size());
        }
    }

    private void clearRuntimeImages() {
        String runtimePrefix = appProperties.getDemoReset().getS3RuntimePrefix();
        Result<Void> result = imageStorageService.deleteImagesByPrefix(runtimePrefix);

        if (result.isFailure()) {
            log.warn("Demo reset completed DB seed but S3 runtime cleanup failed. prefix={}", runtimePrefix);
        }
    }

    private void clearMailpitInbox() {
        if (!appProperties.getDemoReset().isClearMailpit()) {
            return;
        }

        demoEmailSandboxService.clearAllSandboxState();
        MailpitClient mailpitClient = mailpitClientProvider.getIfAvailable();

        if (mailpitClient == null) {
            log.info("Demo reset skipped Mailpit cleanup because Mailpit client is not available");
            return;
        }

        try {
            mailpitClient.deleteAllMessages(MAILPIT_DELETE_BATCH_SIZE, MAILPIT_DELETE_MAX_BATCHES);
        } catch (RuntimeException ex) {
            log.warn("Demo reset completed DB seed but Mailpit cleanup failed. reason={}", ex.getMessage(), ex);
        }
    }
}
