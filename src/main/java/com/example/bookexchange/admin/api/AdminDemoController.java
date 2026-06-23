package com.example.bookexchange.admin.api;

import com.example.bookexchange.common.demoreset.DemoResetService;
import com.example.bookexchange.common.swagger.error_response.BadRequestErrorResponse;
import com.example.bookexchange.common.swagger.error_response.ConflictErrorResponse;
import com.example.bookexchange.common.swagger.error_response.ForbiddenErrorResponse;
import com.example.bookexchange.common.swagger.error_response.UnauthorizedErrorResponse;
import com.example.bookexchange.common.web.ResultResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Administrator services - Demo")
@RestController
@RequiredArgsConstructor
public class AdminDemoController {

    private final DemoResetService demoResetService;
    private final ResultResponseMapper responseMapper;

    @UnauthorizedErrorResponse
    @ForbiddenErrorResponse
    @BadRequestErrorResponse
    @ConflictErrorResponse
    @Operation(
            summary = "Reset demo environment",
            description = "Manually resets mutable demo data, runtime images and Mailpit inbox. Works only when runtime is demo and demo reset is enabled."
    )
    @ApiResponse(responseCode = "200", description = "The demo environment has been reset")
    @PostMapping(AdminPaths.ADMIN_PATH_DEMO_RESET)
    public ResponseEntity<?> resetDemoEnvironment(HttpServletRequest request) {
        return responseMapper.map(demoResetService.resetDemoEnvironment(), request);
    }
}
