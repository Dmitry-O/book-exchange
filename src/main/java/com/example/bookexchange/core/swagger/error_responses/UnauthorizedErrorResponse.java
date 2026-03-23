package com.example.bookexchange.core.swagger.error_responses;

import com.example.bookexchange.core.error.ApiError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class),
                examples = {
                        @ExampleObject(
                                value = """
                                            {
                                              "success": false,
                                              "data": null,
                                              "message": null,
                                              "error": {
                                                "status": 401,
                                                "error": "SYSTEM_INVALID_TOKEN",
                                                "message": "An invalid or expired token was entered",
                                                "path": "/api/v1/**",
                                                "timestamp": "2026-03-21T17:18:26.216Z",
                                                "requestId": "3fa6b455-d7d0-4f90-9a07-df090a324401"
                                              }
                                            }
                                        """
                        )
                }
        )
)
public @interface UnauthorizedErrorResponse {
}
