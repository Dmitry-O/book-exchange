package com.example.bookexchange.report.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.report.dto.ReportCreateDTO;
import com.example.bookexchange.report.model.ReportReason;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.report.repository.ReportRepository;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.user.model.User;
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

import static org.assertj.core.api.Assertions.assertThat;
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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc();
    }

    @Test
    void createUserReport() throws Exception {
        User reporter = userUtil.createUser(700);
        User targetUser = userUtil.createUser(701);
        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.USER)
                .reason(ReportReason.SPAM)
                .comment("This user keeps sending spam offers.")
                .build();

        MvcResult mvcResult = mockMvc.perform(post(ReportPaths.REPORT_PATH_TARGET_ID, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        Report savedReport = reportRepository.findAll().stream().findFirst().orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").isNull()).isTrue();
        assertThat(body.path("message").asText()).isNotBlank();
        assertThat(savedReport.getReporter().getId()).isEqualTo(reporter.getId());
        assertThat(savedReport.getTargetType()).isEqualTo(TargetType.USER);
        assertThat(savedReport.getTargetId()).isEqualTo(targetUser.getId());
        assertThat(savedReport.getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(savedReport.getStatus()).isEqualTo(ReportStatus.OPEN);
    }

    @Test
    void createBookReport() throws Exception {
        User reporter = userUtil.createUser(702);
        User bookOwner = userUtil.createUser(703);
        Long targetBookId = bookUtil.createBook(bookOwner.getId(), 703);
        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.BOOK)
                .reason(ReportReason.INAPPROPRIATE)
                .comment("The book listing contains inappropriate content.")
                .build();

        MvcResult mvcResult = mockMvc.perform(post(ReportPaths.REPORT_PATH_TARGET_ID, targetBookId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reporter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        Report savedReport = reportRepository.findAll().stream().findFirst().orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(savedReport.getTargetType()).isEqualTo(TargetType.BOOK);
        assertThat(savedReport.getTargetId()).isEqualTo(targetBookId);
        assertThat(savedReport.getReason()).isEqualTo(ReportReason.INAPPROPRIATE);
    }

    @Test
    void createReportBadRequest() throws Exception {
        User reporter = userUtil.createUser(704);
        User targetUser = userUtil.createUser(705);
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
    void createReportBadRequestWhenTryingToReportYourself() throws Exception {
        User reporter = userUtil.createUser(706);
        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.USER)
                .reason(ReportReason.SPAM)
                .comment("Trying to report myself should fail.")
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
    void createReportBadRequestWhenTryingToReportYourOwnBook() throws Exception {
        User reporter = userUtil.createUser(707);
        Long ownBookId = bookUtil.createBook(reporter.getId(), 707);
        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.BOOK)
                .reason(ReportReason.OTHER)
                .comment("Trying to report my own book should fail.")
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
    void createReportNotFoundWhenTargetUserDoesNotExist() throws Exception {
        User reporter = userUtil.createUser(708);
        ReportCreateDTO dto = ReportCreateDTO.builder()
                .targetType(TargetType.USER)
                .reason(ReportReason.FRAUD)
                .comment("This target user does not exist.")
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
}
