package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.core.swagger.error_responses.BadRequestErrorResponse;
import com.example.bookexchange.core.swagger.error_responses.NotFoundErrorResponse;
import com.example.bookexchange.core.swagger.error_responses.UnauthorizedErrorResponse;
import com.example.bookexchange.core.web.ResultResponseMapper;
import com.example.bookexchange.dto.ReportCreateDTO;
import com.example.bookexchange.services.ReportService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports")
@RestController
@AllArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ResultResponseMapper responseMapper;

    public static final String REPORT_PATH_TARGET_ID = "/api/v1/report/{targetId}";

    @UnauthorizedErrorResponse
    @BadRequestErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
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
    @PostMapping(REPORT_PATH_TARGET_ID)
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
