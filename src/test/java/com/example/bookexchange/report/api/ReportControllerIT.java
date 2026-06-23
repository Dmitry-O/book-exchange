package com.example.bookexchange.report.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.report.dto.ReportCreateDTO;
import com.example.bookexchange.report.model.ReportReason;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.report.repository.ReportRepository;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.support.FixtureNumbers;
import com.example.bookexchange.support.TestReportStrings;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.support.fixture.BookFixtureSupport;
import com.example.bookexchange.support.fixture.UserFixtureSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class ReportControllerIT extends IntegrationTestSupport {

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    UserFixtureSupport userUtil;

    @Autowired
    BookFixtureSupport bookUtil;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BookRepository bookRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldCreateUserReport_whenPayloadIsValid() throws Exception {
        User reporter = userUtil.createUser(FixtureNumbers.report(700));
        User targetUser = userUtil.createUser(FixtureNumbers.report(701));
        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.USER)
                .reason(ReportReason.SPAM)
                .comment(TestReportStrings.comment("This user keeps sending spam offers."))
                .build();

        MvcResult mvcResult = mockMvc.perform(post(ReportPaths.REPORT_PATH_TARGET_ID, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        Report savedReport = findReport(reporter, TargetType.USER, targetUser.getId(), ReportStatus.OPEN);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").isNull()).isTrue();
        assertThat(body.path("message").asText()).isNotBlank();
        assertThat(savedReport.getReporter().getId()).isEqualTo(reporter.getId());
        assertThat(savedReport.getTargetType()).isEqualTo(TargetType.USER);
        assertThat(savedReport.getTargetId()).isEqualTo(targetUser.getId());
        assertThat(savedReport.getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(savedReport.getStatus()).isEqualTo(ReportStatus.OPEN);
        assertThat(savedReport.getTargetUserNicknameSnapshot()).isEqualTo(targetUser.getNickname());
        assertThat(savedReport.getTargetBookNameSnapshot()).isNull();
    }

    @Test
    void shouldCreateBookReport_whenPayloadIsValid() throws Exception {
        User reporter = userUtil.createUser(FixtureNumbers.report(702));
        User bookOwner = userUtil.createUser(FixtureNumbers.report(703));
        Long targetBookId = bookUtil.createBook(bookOwner.getId(), FixtureNumbers.report(703));
        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.BOOK)
                .reason(ReportReason.INAPPROPRIATE)
                .comment(TestReportStrings.comment("The book listing contains inappropriate content."))
                .build();

        MvcResult mvcResult = mockMvc.perform(post(ReportPaths.REPORT_PATH_TARGET_ID, targetBookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        Report savedReport = findReport(reporter, TargetType.BOOK, targetBookId, ReportStatus.OPEN);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(savedReport.getTargetType()).isEqualTo(TargetType.BOOK);
        assertThat(savedReport.getTargetId()).isEqualTo(targetBookId);
        assertThat(savedReport.getReason()).isEqualTo(ReportReason.INAPPROPRIATE);
        assertThat(savedReport.getTargetBookNameSnapshot()).isNotBlank();
        assertThat(savedReport.getTargetBookOwnerUserIdSnapshot()).isEqualTo(bookOwner.getId());
        assertThat(savedReport.getTargetBookOwnerNicknameSnapshot()).isEqualTo(bookOwner.getNickname());
    }

    @Test
    void shouldReturnConflict_whenUserCreatesDuplicateReportForSameTarget() throws Exception {
        User reporter = userUtil.createUser(FixtureNumbers.report(709));
        User targetUser = userUtil.createUser(FixtureNumbers.report(710));
        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.USER)
                .reason(ReportReason.SPAM)
                .comment(TestReportStrings.comment("This user keeps sending spam offers."))
                .build();

        mockMvc.perform(post(ReportPaths.REPORT_PATH_TARGET_ID, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        MvcResult mvcResult = mockMvc.perform(post(ReportPaths.REPORT_PATH_TARGET_ID, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                409,
                MessageKey.REPORT_ALREADY_EXISTS,
                reportPath(targetUser.getId())
        );
    }

    @Test
    void shouldAllowCreatingNewReport_whenPreviousReportForSameTargetIsAlreadyResolved() throws Exception {
        User reporter = userUtil.createUser(FixtureNumbers.report(711));
        User targetUser = userUtil.createUser(FixtureNumbers.report(712));
        Report resolvedReport = new Report(
                null,
                TargetType.USER,
                targetUser.getId(),
                ReportReason.SPAM,
                "Old resolved report",
                ReportStatus.RESOLVED,
                reporter
        );
        resolvedReport.captureUserSnapshot(targetUser);
        reportRepository.save(resolvedReport);

        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.USER)
                .reason(ReportReason.FRAUD)
                .comment(TestReportStrings.comment("A new issue appeared after the previous report was resolved."))
                .build();

        MvcResult mvcResult = mockMvc.perform(post(ReportPaths.REPORT_PATH_TARGET_ID, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        List<Report> targetReports = findReports(reporter, TargetType.USER, targetUser.getId());

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(targetReports).hasSize(2);
        assertThat(targetReports.stream().map(Report::getStatus))
                .containsExactlyInAnyOrder(ReportStatus.RESOLVED, ReportStatus.OPEN);
    }

    @Test
    void shouldReturnCurrentUserReports_whenUserGetsOwnReports() throws Exception {
        User reporter = userUtil.createUser(FixtureNumbers.report(720));
        User targetUser = userUtil.createUser(FixtureNumbers.report(721));
        targetUser.setPhotoUrl("https://cdn.example.com/users/721/profile.jpg");
        userRepository.save(targetUser);
        User anotherReporter = userUtil.createUser(FixtureNumbers.report(722));
        Long targetBookId = bookUtil.createBook(targetUser.getId(), FixtureNumbers.report(723));

        Report userReportEntity = new Report(
                null,
                TargetType.USER,
                targetUser.getId(),
                ReportReason.SPAM,
                "User report",
                ReportStatus.OPEN,
                reporter
        );
        userReportEntity.captureUserSnapshot(targetUser);
        reportRepository.save(userReportEntity);

        Book targetBook = bookRepository.findById(targetBookId).orElseThrow();
        Report bookReportEntity = new Report(
                null,
                TargetType.BOOK,
                targetBookId,
                ReportReason.FRAUD,
                "Book report",
                ReportStatus.RESOLVED,
                reporter
        );
        bookReportEntity.captureBookSnapshot(targetBook);
        reportRepository.save(bookReportEntity);

        reportRepository.save(new Report(
                null,
                TargetType.USER,
                reporter.getId(),
                ReportReason.OTHER,
                "Another reporter entry",
                ReportStatus.OPEN,
                anotherReporter
        ));

        MvcResult mvcResult = mockMvc.perform(get(ReportPaths.REPORT_PATH_USER)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .queryParam("pageIndex", "0")
                        .queryParam("pageSize", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode content = body.path("data").path("content");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(2);
        assertThat(content).hasSize(2);
        assertHasVersion(content.get(0));
        JsonNode userReport = findReportByTargetType(content, TargetType.USER);
        JsonNode bookReport = findReportByTargetType(content, TargetType.BOOK);

        assertThat(userReport.path("targetUser").path("id").asLong()).isEqualTo(targetUser.getId());
        assertThat(userReport.path("targetUser").path("nickname").asText()).isEqualTo(targetUser.getNickname());
        assertThat(userReport.path("targetUser").path("photoUrl").asText()).isEqualTo(targetUser.getPhotoUrl());
        assertThat(userReport.path("targetDeleted").asBoolean()).isFalse();
        assertThat(userReport.path("targetBook").isNull()).isTrue();

        assertThat(bookReport.path("targetBook").path("id").asLong()).isEqualTo(targetBookId);
        assertThat(bookReport.path("targetBook").path("name").asText()).isNotBlank();
        assertThat(bookReport.path("targetBook").path("photoUrl").asText()).isNotBlank();
        assertThat(bookReport.path("targetBook").path("ownerUserId").asLong()).isEqualTo(targetUser.getId());
        assertThat(bookReport.path("targetBook").path("ownerNickname").asText()).isEqualTo(targetUser.getNickname());
        assertThat(bookReport.path("targetBook").path("ownerPhotoUrl").asText()).isEqualTo(targetUser.getPhotoUrl());
        assertThat(bookReport.path("targetDeleted").asBoolean()).isFalse();
        assertThat(bookReport.path("targetUser").isNull()).isTrue();
    }

    @Test
    void shouldReturnHistoricalSnapshotAndDeletedFlag_whenReportedTargetWasDeleted() throws Exception {
        User reporter = userUtil.createUser(FixtureNumbers.report(730));
        User targetUser = userUtil.createUser(FixtureNumbers.report(731));
        targetUser.setPhotoUrl("https://cdn.example.com/users/731/profile.jpg");
        userRepository.save(targetUser);

        Long targetBookId = bookUtil.createBook(targetUser.getId(), FixtureNumbers.report(732));
        Book targetBook = bookRepository.findById(targetBookId).orElseThrow();
        String userSnapshotNickname = targetUser.getNickname();
        String bookSnapshotName = targetBook.getName();

        Report userReportEntity = new Report(
                null,
                TargetType.USER,
                targetUser.getId(),
                ReportReason.SPAM,
                "User report",
                ReportStatus.OPEN,
                reporter
        );
        userReportEntity.captureUserSnapshot(targetUser);
        reportRepository.save(userReportEntity);

        Report bookReportEntity = new Report(
                null,
                TargetType.BOOK,
                targetBookId,
                ReportReason.FRAUD,
                "Book report",
                ReportStatus.OPEN,
                reporter
        );
        bookReportEntity.captureBookSnapshot(targetBook);
        reportRepository.save(bookReportEntity);

        targetUser.setDeletedAt(Instant.parse("2026-04-24T08:15:30Z"));
        targetUser.setEmail("deleted_731@example.com");
        targetUser.setNickname("deleted_user_731");
        targetUser.setPhotoUrl(null);
        userRepository.save(targetUser);

        targetBook.setDeletedAt(Instant.parse("2026-04-24T08:15:31Z"));
        targetBook.setName("Deleted book");
        targetBook.setPhotoUrl(null);
        bookRepository.save(targetBook);

        clearPersistenceContext();

        MvcResult mvcResult = mockMvc.perform(get(ReportPaths.REPORT_PATH_USER)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .queryParam("pageIndex", "0")
                        .queryParam("pageSize", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = responseBody(mvcResult).path("data").path("content");
        JsonNode userReport = findReportByTargetType(content, TargetType.USER);
        JsonNode bookReport = findReportByTargetType(content, TargetType.BOOK);

        assertThat(userReport.path("targetDeleted").asBoolean()).isTrue();
        assertThat(userReport.path("targetUser").path("nickname").asText()).isEqualTo(userSnapshotNickname);
        assertThat(userReport.path("targetUser").path("photoUrl").isNull()).isTrue();

        assertThat(bookReport.path("targetDeleted").asBoolean()).isTrue();
        assertThat(bookReport.path("targetBook").path("name").asText()).isEqualTo(bookSnapshotName);
        assertThat(bookReport.path("targetBook").path("photoUrl").isNull()).isTrue();
        assertThat(bookReport.path("targetBook").path("ownerNickname").asText()).isEqualTo(userSnapshotNickname);
        assertThat(bookReport.path("targetBook").path("ownerPhotoUrl").isNull()).isTrue();
    }

    @Test
    void shouldReturnBadRequest_whenCreateReportPayloadIsInvalid() throws Exception {
        User reporter = userUtil.createUser(FixtureNumbers.report(704));
        User targetUser = userUtil.createUser(FixtureNumbers.report(705));
        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.USER)
                .reason(ReportReason.SPAM)
                .comment(" ")
                .build();

        MvcResult mvcResult = mockMvc.perform(post(ReportPaths.REPORT_PATH_TARGET_ID, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertValidationErrorResponse(responseBody(mvcResult), reportPath(targetUser.getId()));
    }

    @Test
    void shouldReturnBadRequest_whenUserReportsThemself() throws Exception {
        User reporter = userUtil.createUser(FixtureNumbers.report(706));
        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.USER)
                .reason(ReportReason.SPAM)
                .comment(TestReportStrings.comment("Trying to report myself should fail."))
                .build();

        MvcResult mvcResult = mockMvc.perform(post(ReportPaths.REPORT_PATH_TARGET_ID, reporter.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.REPORT_CANNOT_REPORT_YOURSELF,
                reportPath(reporter.getId())
        );
    }

    @Test
    void shouldReturnBadRequest_whenUserReportsOwnBook() throws Exception {
        User reporter = userUtil.createUser(FixtureNumbers.report(707));
        Long ownBookId = bookUtil.createBook(reporter.getId(), FixtureNumbers.report(707));
        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.BOOK)
                .reason(ReportReason.OTHER)
                .comment(TestReportStrings.comment("Trying to report my own book should fail."))
                .build();

        MvcResult mvcResult = mockMvc.perform(post(ReportPaths.REPORT_PATH_TARGET_ID, ownBookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.REPORT_CANNOT_REPORT_YOUR_BOOK,
                reportPath(ownBookId)
        );
    }

    @Test
    void shouldReturnNotFound_whenCreateUserReportTargetDoesNotExist() throws Exception {
        User reporter = userUtil.createUser(FixtureNumbers.report(708));
        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.USER)
                .reason(ReportReason.FRAUD)
                .comment(TestReportStrings.comment("This target user does not exist."))
                .build();

        MvcResult mvcResult = mockMvc.perform(post(ReportPaths.REPORT_PATH_TARGET_ID, Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.USER_ACCOUNT_NOT_FOUND,
                reportPath(Long.MAX_VALUE)
        );
    }

    private String reportPath(Long targetId) {
        return ReportPaths.REPORT_PATH + "/" + targetId;
    }

    private JsonNode findReportByTargetType(JsonNode content, TargetType targetType) {
        for (JsonNode item : content) {
            if (item.path("targetType").asText().equals(targetType.name())) {
                return item;
            }
        }

        throw new IllegalStateException("Report with targetType=" + targetType + " was not found in response");
    }

    private Report findReport(User reporter, TargetType targetType, Long targetId, ReportStatus status) {
        return findReports(reporter, targetType, targetId).stream()
                .filter(report -> report.getStatus() == status)
                .findFirst()
                .orElseThrow();
    }

    private List<Report> findReports(User reporter, TargetType targetType, Long targetId) {
        return reportRepository.findAll().stream()
                .filter(report -> report.getReporter().getId().equals(reporter.getId()))
                .filter(report -> report.getTargetType() == targetType)
                .filter(report -> report.getTargetId().equals(targetId))
                .toList();
    }
}
