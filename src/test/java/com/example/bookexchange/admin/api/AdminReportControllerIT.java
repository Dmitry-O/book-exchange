package com.example.bookexchange.admin.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.common.dto.SortDirectionDTO;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.report.model.ReportReason;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.report.repository.ReportRepository;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.support.PageTestDefaults;
import com.example.bookexchange.support.FixtureNumbers;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class AdminReportControllerIT extends IntegrationTestSupport {

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    UserFixtureSupport userUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldReturnFilteredReports_whenAdminGetsReportsByStatus() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminReport(1));
        Report openReport = createUserReport(FixtureNumbers.adminReport(2), ReportStatus.OPEN);
        createUserReport(FixtureNumbers.adminReport(10), ReportStatus.RESOLVED);

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_REPORTS)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("reportStatuses", ReportStatus.OPEN.name())
                        .queryParam("sortDirection", SortDirectionDTO.ASC.name())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode content = body.path("data").path("content");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(openReport.getId());
        assertHasVersion(content.get(0));
        assertThat(content.get(0).path("status").asText()).isEqualTo(ReportStatus.OPEN.name());
    }

    @Test
    void shouldReturnReport_whenAdminGetsReportById() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminReport(20));
        Report report = createUserReport(FixtureNumbers.adminReport(21), ReportStatus.OPEN);

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_REPORTS_ID, report.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(report.getId());
        assertThat(body.path("data").path("status").asText()).isEqualTo(ReportStatus.OPEN.name());
        assertThat(body.path("data").path("targetId").asLong()).isEqualTo(report.getTargetId());
    }

    @Test
    void shouldReturnNotFound_whenAdminGetsMissingReportById() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminReport(22));

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_REPORTS_ID, Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.ADMIN_REPORT_NOT_FOUND,
                AdminPaths.ADMIN_PATH_REPORTS + "/" + Long.MAX_VALUE
        );
    }

    @Test
    void shouldResolveReport_whenAdminResolvesReport() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminReport(30));
        Report report = createUserReport(FixtureNumbers.adminReport(31), ReportStatus.OPEN);

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_REPORTS_ID_RESOLVE, report.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, ifMatch(report.getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        Report resolvedReport = reportRepository.findById(report.getId()).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("status").asText()).isEqualTo(ReportStatus.RESOLVED.name());
        assertThat(resolvedReport.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void shouldRejectReport_whenAdminRejectsReport() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminReport(40));
        Report report = createUserReport(FixtureNumbers.adminReport(41), ReportStatus.OPEN);

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_REPORTS_ID_REJECT, report.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, ifMatch(report.getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        Report rejectedReport = reportRepository.findById(report.getId()).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("status").asText()).isEqualTo(ReportStatus.REJECTED.name());
        assertThat(rejectedReport.getStatus()).isEqualTo(ReportStatus.REJECTED);
    }

    @Test
    void shouldReturnConflict_whenAdminResolvesReportWithStaleVersion() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminReport(50));
        Report report = createUserReport(FixtureNumbers.adminReport(51), ReportStatus.OPEN);

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_REPORTS_ID_RESOLVE, report.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, "\"999\"")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                409,
                MessageKey.SYSTEM_OPTIMISTIC_LOCK,
                AdminPaths.ADMIN_PATH_REPORTS + "/" + report.getId() + "/resolve"
        );
    }

    @Test
    void shouldReturnConflict_whenAdminRejectsReportWithStaleVersion() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminReport(52));
        Report report = createUserReport(FixtureNumbers.adminReport(53), ReportStatus.OPEN);

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_REPORTS_ID_REJECT, report.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, "\"999\"")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                409,
                MessageKey.SYSTEM_OPTIMISTIC_LOCK,
                AdminPaths.ADMIN_PATH_REPORTS + "/" + report.getId() + "/reject"
        );
    }

    private Report createUserReport(int seed, ReportStatus status) {
        User reporter = userUtil.createUser(seed);
        User targetUser = userUtil.createUser(seed + 1);

        return reportRepository.save(new Report(
                null,
                TargetType.USER,
                targetUser.getId(),
                ReportReason.SPAM,
                "Admin report seed " + seed,
                status,
                reporter
        ));
    }
}
