package com.example.bookexchange.common.demoaccess;

import com.example.bookexchange.common.swagger.error_response.BadRequestErrorResponse;
import com.example.bookexchange.common.web.ApiResponse;
import com.example.bookexchange.common.web.ResultResponseMapper;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Demo Access")
@RestController
@RequiredArgsConstructor
@Validated
public class DemoAccessController {

    private final DemoAccessService demoAccessService;
    private final ResultResponseMapper responseMapper;

    @BadRequestErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Verifies the portfolio demo access token and issues an HttpOnly demo access cookie",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "enabled": true,
                                        "expiresAt": "2026-06-19T12:30:00Z"
                                      },
                                      "message": null,
                                      "error": null
                                    }
                                    """
                    )
            )
    )
    @PostMapping(DemoAccessPaths.DEMO_ACCESS_VERIFY_PATH)
    public ResponseEntity<?> verifyAccessToken(
            @Validated @RequestBody DemoAccessVerifyRequestDTO dto,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return responseMapper.map(demoAccessService.verifyAccessToken(dto.token(), response), request);
    }
}
