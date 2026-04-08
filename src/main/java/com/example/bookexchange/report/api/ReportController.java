package com.example.bookexchange.report.api;

import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.security.auth.CurrentUser;
import com.example.bookexchange.common.swagger.page_data_response.ReportPageData;
import com.example.bookexchange.common.swagger.error_response.BadRequestErrorResponse;
import com.example.bookexchange.common.swagger.error_response.NotFoundErrorResponse;
import com.example.bookexchange.common.swagger.error_response.UnauthorizedErrorResponse;
import com.example.bookexchange.common.web.ApiResponse;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.report.dto.ReportCreateDTO;
import com.example.bookexchange.report.service.ReportService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports")
@RestController
@AllArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ResultResponseMapper responseMapper;

    @UnauthorizedErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "A list of returned user's reports",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReportPageData.class)
            )
    )
    @GetMapping(ReportPaths.REPORT_PATH_USER)
    public ResponseEntity<?> getUserReports(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @ParameterObject
            @Validated @org.springframework.web.bind.annotation.ModelAttribute PageQueryDTO queryDTO,

            HttpServletRequest request
    ) {
        return responseMapper.map(reportService.findUserReports(userId, queryDTO), request);
    }

    @UnauthorizedErrorResponse
    @BadRequestErrorResponse
    @NotFoundErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "User's report has been submitted",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "Your report has been sent",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PostMapping(ReportPaths.REPORT_PATH_TARGET_ID)
    public ResponseEntity<?> createReport(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Target ID", example = "1")
            @PathVariable Long targetId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Report data for the report creation", required = true)
            @Valid @RequestBody ReportCreateDTO reportCreateDTO,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                reportService.createReport(
                        userId,
                        targetId,
                        reportCreateDTO
                ),
                request
        );
    }
}
