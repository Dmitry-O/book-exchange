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
        responseCode = "404",
        description = "Not found",
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
                                                "status": 404,
                                                "error": "NOT_FOUND",
                                                "message": "Requested resource was not found",
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
public @interface NotFoundErrorResponse {
}
