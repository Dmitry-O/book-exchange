package com.example.bookexchange.admin.api;

import com.example.bookexchange.admin.dto.ReportAdminDTO;
import com.example.bookexchange.admin.service.AdminReportService;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.dto.SortDirectionDTO;
import com.example.bookexchange.common.swagger.error_response.*;
import com.example.bookexchange.common.swagger.page_data_response.ReportAdminPageData;
import com.example.bookexchange.common.util.ParserUtil;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.report.model.ReportStatus;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "Administrator services - Reports")
@RestController
@AllArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;
    private ParserUtil parserUtil;
    private final ResultResponseMapper responseMapper;

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @BadRequestErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "A list of returned users' reports",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReportAdminPageData.class)
            )
    )
    @GetMapping(AdminPaths.ADMIN_PATH_REPORTS)
    public ResponseEntity<?> adminGetReports(
            @ParameterObject
            @Validated @ModelAttribute PageQueryDTO queryDTO,

            @Parameter(description = "Report statuses", example = "OPEN,RESOLVED,REJECTED")
            @RequestParam(value = "reportStatuses", required = false) Set<ReportStatus> reportStatuses,

            @Parameter(description = "Sort direction", example = "DESC")
            @RequestParam(value = "sortDirection", defaultValue = "ASC") SortDirectionDTO sortDirection,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminReportService.findReports(
                        queryDTO,
                        reportStatuses,
                        sortDirection
                ),
                request
        );
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The returned user's report",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReportAdminDTO.class)
            )
    )
    @GetMapping(AdminPaths.ADMIN_PATH_REPORTS_ID)
    public ResponseEntity<?> adminGetReportById(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "Report ID", example = "1")
            @PathVariable Long reportId,

            HttpServletRequest request
    ) {
        return responseMapper.map(adminReportService.findReportById(adminUser, reportId), request);
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The report has been resolved",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReportAdminDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "targetType": "USER",
                                            "targetId": 1,
                                            "reason": "SPAM",
                                            "comment": "This user spams a lot with same books",
                                            "status": "RESOLVED",
                                            "reporter": {
                                              "id": 1,
                                              "email": "example@info.com",
                                              "nickname": "user_12345",
                                              "photoBase64": "User photo",
                                              "bannedUntil": null,
                                              "bannedPermanently": false,
                                              "banReason": null,
                                              "roles": ["USER"],
                                              "locale": "de"
                                            },
                                            "meta": {
                                              "createdAt": "2026-03-23T13:00:58.815Z",
                                              "updatedAt": "2026-03-23T14:00:58.815Z",
                                              "deletedAt": null,
                                              "createdBy": 1,
                                              "updatedBy": 2,
                                              "createdRequestId": "4fa6b455-d7d0-4f90-9a07-df090a324401",
                                              "updatedRequestId": "3fa6b455-d7d0-4f90-9a07-df090a324401"
                                            }
                                          },
                                          "message": "The report has been resolved",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AdminPaths.ADMIN_PATH_REPORTS_ID_RESOLVE)
    public ResponseEntity<?> adminReportResolve(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "Report ID", example = "1")
            @PathVariable Long reportId,

            @Parameter(
                    name = "If-Match",
                    description = "Entity version for optimistic locking",
                    example = "\"3\"",
                    required = true
            )
            @RequestHeader("If-Match") String ifMatch,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminReportService.resolveReport(
                        adminUser,
                        reportId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The report has been rejected",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReportAdminDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "targetType": "USER",
                                            "targetId": 1,
                                            "reason": "SPAM",
                                            "comment": "This user has terrible books",
                                            "status": "REJECTED",
                                            "reporter": {
                                              "id": 1,
                                              "email": "example@info.com",
                                              "nickname": "user_12345",
                                              "photoBase64": "User photo",
                                              "bannedUntil": null,
                                              "bannedPermanently": false,
                                              "banReason": null,
                                              "roles": ["USER"],
                                              "locale": "de"
                                            },
                                            "meta": {
                                              "createdAt": "2026-03-23T13:00:58.815Z",
                                              "updatedAt": "2026-03-23T14:00:58.815Z",
                                              "deletedAt": null,
                                              "createdBy": 1,
                                              "updatedBy": 2,
                                              "createdRequestId": "4fa6b455-d7d0-4f90-9a07-df090a324401",
                                              "updatedRequestId": "3fa6b455-d7d0-4f90-9a07-df090a324401"
                                            }
                                          },
                                          "message": "The report has been rejected",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AdminPaths.ADMIN_PATH_REPORTS_ID_REJECT)
    public ResponseEntity<?> adminReportReject(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "Report ID", example = "1")
            @PathVariable Long reportId,

            @Parameter(
                    name = "If-Match",
                    description = "Entity version for optimistic locking",
                    example = "\"3\"",
                    required = true
            )
            @RequestHeader("If-Match") String ifMatch,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminReportService.rejectReport(
                        adminUser,
                        reportId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }
}
