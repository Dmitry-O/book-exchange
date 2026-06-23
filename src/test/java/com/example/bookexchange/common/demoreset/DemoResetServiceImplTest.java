package com.example.bookexchange.common.demoreset;

import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.book.search.BookSearchIndexService;
import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.demoemail.DemoEmailSandboxService;
import com.example.bookexchange.common.demoemail.MailpitClient;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.common.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_BOOK_SEARCH_REINDEXED;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_DEMO_RESET_COMPLETED;
import static com.example.bookexchange.common.i18n.MessageKey.SYSTEM_DEMO_RESET_DISABLED;
import static com.example.bookexchange.common.i18n.MessageKey.SYSTEM_INVALID_DATA;
import static com.example.bookexchange.common.i18n.MessageKey.SYSTEM_UNEXPECTED_ERROR;
import static com.example.bookexchange.common.i18n.MessageKey.SYSTEM_DEMO_RESET_SEED_NOT_CONFIGURED;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(MockitoExtension.class)
class DemoResetServiceImplTest {

    @Mock
    private DemoSeedImporter demoSeedImporter;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private ObjectProvider<MailpitClient> mailpitClientProvider;

    @Mock
    private MailpitClient mailpitClient;

    @Mock
    private DemoEmailSandboxService demoEmailSandboxService;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookSearchIndexService bookSearchIndexService;

    private AppProperties appProperties;
    private StorageProperties storageProperties;
    private DemoMaintenanceService demoMaintenanceService;
    private DemoResetServiceImpl demoResetService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setRuntimeEnv("demo");
        appProperties.getDemoReset().setEnabled(true);
        appProperties.getDemoReset().setSeedS3Key("private/demo-reset/demo-seed-export.sql");
        appProperties.getDemoReset().setS3RuntimePrefix("runtime/");

        storageProperties = new StorageProperties();
        storageProperties.getS3().setProdBucket("book-exchange-demo-images");
        storageProperties.getS3().setUseTestBucket(false);

        demoMaintenanceService = new DemoMaintenanceService();
        demoResetService = new DemoResetServiceImpl(
                appProperties,
                demoMaintenanceService,
                demoSeedImporter,
                jdbcTemplate,
                imageStorageService,
                storageProperties,
                mailpitClientProvider,
                demoEmailSandboxService,
                bookRepository,
                bookSearchIndexService
        );
    }

    @Test
    void shouldResetDemoEnvironment_whenRuntimeIsDemoAndResetIsEnabled() {
        when(bookRepository.findAllForSearchIndex()).thenReturn(List.of());
        when(bookSearchIndexService.reindexAll(List.of())).thenReturn(ResultFactory.okMessage(ADMIN_BOOK_SEARCH_REINDEXED, 0));
        when(imageStorageService.deleteImagesByPrefix("runtime/")).thenReturn(ResultFactory.successVoid());
        when(mailpitClientProvider.getIfAvailable()).thenReturn(mailpitClient);

        assertSuccess(demoResetService.resetDemoEnvironment(), OK, ADMIN_DEMO_RESET_COMPLETED);

        InOrder inOrder = inOrder(jdbcTemplate, demoSeedImporter, bookRepository, imageStorageService, demoEmailSandboxService, mailpitClient);
        inOrder.verify(demoSeedImporter).validateSeedAvailable("s3://book-exchange-demo-images/private/demo-reset/demo-seed-export.sql");
        inOrder.verify(jdbcTemplate).execute("SET FOREIGN_KEY_CHECKS = 0");
        inOrder.verify(jdbcTemplate).execute("TRUNCATE TABLE `user_update`");
        inOrder.verify(jdbcTemplate).execute("TRUNCATE TABLE `verification_token`");
        inOrder.verify(jdbcTemplate).execute("TRUNCATE TABLE `refresh_token`");
        inOrder.verify(jdbcTemplate).execute("TRUNCATE TABLE `report`");
        inOrder.verify(jdbcTemplate).execute("TRUNCATE TABLE `exchange`");
        inOrder.verify(jdbcTemplate).execute("TRUNCATE TABLE `book`");
        inOrder.verify(jdbcTemplate).execute("TRUNCATE TABLE `user_roles`");
        inOrder.verify(jdbcTemplate).execute("TRUNCATE TABLE `app_user`");
        inOrder.verify(jdbcTemplate).execute("SET FOREIGN_KEY_CHECKS = 1");
        inOrder.verify(demoSeedImporter).importSeed("s3://book-exchange-demo-images/private/demo-reset/demo-seed-export.sql");
        inOrder.verify(bookRepository).findAllForSearchIndex();
        inOrder.verify(imageStorageService).deleteImagesByPrefix("runtime/");
        inOrder.verify(demoEmailSandboxService).clearAllSandboxState();
        inOrder.verify(mailpitClient).deleteAllMessages(1000, 100);

        assertThat(demoMaintenanceService.isMaintenanceMode()).isFalse();
    }

    @Test
    void shouldSkipReset_whenRuntimeIsNotDemo() {
        appProperties.setRuntimeEnv("local");

        assertFailure(demoResetService.resetDemoEnvironment(), SYSTEM_DEMO_RESET_DISABLED, BAD_REQUEST);

        verify(jdbcTemplate, never()).execute("SET FOREIGN_KEY_CHECKS = 0");
        verify(demoSeedImporter, never()).validateSeedAvailable("s3://book-exchange-demo-images/private/demo-reset/demo-seed-export.sql");
        verify(demoSeedImporter, never()).importSeed("s3://book-exchange-demo-images/private/demo-reset/demo-seed-export.sql");
        assertThat(demoMaintenanceService.isMaintenanceMode()).isFalse();
    }

    @Test
    void shouldSkipReset_whenSeedS3KeyIsNotConfigured() {
        appProperties.getDemoReset().setSeedS3Key(null);

        assertFailure(demoResetService.resetDemoEnvironment(), SYSTEM_DEMO_RESET_SEED_NOT_CONFIGURED, BAD_REQUEST);

        verify(jdbcTemplate, never()).execute("SET FOREIGN_KEY_CHECKS = 0");
        verify(demoSeedImporter, never()).validateSeedAvailable("s3://book-exchange-demo-images/private/demo-reset/demo-seed-export.sql");
        verify(demoSeedImporter, never()).importSeed("s3://book-exchange-demo-images/private/demo-reset/demo-seed-export.sql");
        assertThat(demoMaintenanceService.isMaintenanceMode()).isFalse();
    }

    @Test
    void shouldSkipReset_whenSeedS3KeyIsInvalid() {
        appProperties.getDemoReset().setSeedS3Key("/");

        assertFailure(demoResetService.resetDemoEnvironment(), SYSTEM_INVALID_DATA, BAD_REQUEST);

        verify(jdbcTemplate, never()).execute("SET FOREIGN_KEY_CHECKS = 0");
        verify(demoSeedImporter, never()).importSeed("s3://book-exchange-demo-images/");
        assertThat(demoMaintenanceService.isMaintenanceMode()).isFalse();
    }

    @Test
    void shouldSkipResetWithoutClearingTables_whenSeedPreflightFails() {
        doThrow(new IllegalStateException("bucket missing"))
                .when(demoSeedImporter)
                .validateSeedAvailable("s3://book-exchange-demo-images/private/demo-reset/demo-seed-export.sql");

        assertFailure(demoResetService.resetDemoEnvironment(), SYSTEM_UNEXPECTED_ERROR, INTERNAL_SERVER_ERROR);

        verify(jdbcTemplate, never()).execute("SET FOREIGN_KEY_CHECKS = 0");
        verify(demoSeedImporter, never()).importSeed("s3://book-exchange-demo-images/private/demo-reset/demo-seed-export.sql");
        assertThat(demoMaintenanceService.isMaintenanceMode()).isFalse();
    }

    @Test
    void shouldDisableMaintenanceAndRestoreForeignKeyChecks_whenSeedImportFails() {
        doThrow(new IllegalStateException("seed broken"))
                .when(demoSeedImporter)
                .importSeed("s3://book-exchange-demo-images/private/demo-reset/demo-seed-export.sql");

        assertFailure(demoResetService.resetDemoEnvironment(), SYSTEM_UNEXPECTED_ERROR, INTERNAL_SERVER_ERROR);

        verify(jdbcTemplate).execute("SET FOREIGN_KEY_CHECKS = 1");
        assertThat(demoMaintenanceService.isMaintenanceMode()).isFalse();
    }

    @Test
    void shouldResolveSeedFromTestBucket_whenUseTestBucketIsEnabled() {
        storageProperties.getS3().setUseTestBucket(true);
        storageProperties.getS3().setTestBucket("book-exchange-demo-test-images");
        when(bookRepository.findAllForSearchIndex()).thenReturn(List.of());
        when(bookSearchIndexService.reindexAll(List.of())).thenReturn(ResultFactory.okMessage(ADMIN_BOOK_SEARCH_REINDEXED, 0));
        when(imageStorageService.deleteImagesByPrefix("runtime/")).thenReturn(ResultFactory.successVoid());

        assertSuccess(demoResetService.resetDemoEnvironment(), OK, ADMIN_DEMO_RESET_COMPLETED);

        verify(demoSeedImporter).validateSeedAvailable("s3://book-exchange-demo-test-images/private/demo-reset/demo-seed-export.sql");
        verify(demoSeedImporter).importSeed("s3://book-exchange-demo-test-images/private/demo-reset/demo-seed-export.sql");
    }

    @Test
    void shouldContinueReset_whenRuntimeImageCleanupFails() {
        when(bookRepository.findAllForSearchIndex()).thenReturn(List.of());
        when(bookSearchIndexService.reindexAll(List.of())).thenReturn(ResultFactory.okMessage(ADMIN_BOOK_SEARCH_REINDEXED, 0));
        when(imageStorageService.deleteImagesByPrefix("runtime/")).thenReturn(ResultFactory.error(SYSTEM_INVALID_DATA, BAD_REQUEST));
        when(mailpitClientProvider.getIfAvailable()).thenReturn(mailpitClient);

        assertSuccess(demoResetService.resetDemoEnvironment(), OK, ADMIN_DEMO_RESET_COMPLETED);

        verify(mailpitClient).deleteAllMessages(1000, 100);
        assertThat(demoMaintenanceService.isMaintenanceMode()).isFalse();
    }
}
