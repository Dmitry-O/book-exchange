package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.core.swagger.error_responses.BadRequestErrorResponse;
import com.example.bookexchange.core.swagger.error_responses.ConflictErrorResponse;
import com.example.bookexchange.core.swagger.error_responses.NotFoundErrorResponse;
import com.example.bookexchange.core.swagger.error_responses.UnauthorizedErrorResponse;
import com.example.bookexchange.core.web.ResultResponseMapper;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.services.UserService;
import com.example.bookexchange.util.ParserUtil;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users")
@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserService userService;
    private final ParserUtil parserUtil;
    private final ResultResponseMapper responseMapper;

    public static final String USER_PATH = "/api/v1/user";
    public static final String USER_PATH_RESET_PASSWORD = USER_PATH + "/reset_password";
    public static final String USER_PATH_LOGOUT = USER_PATH + "/logout";

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "User's profile has been returned",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserDTO.class)
            )
    )
    @GetMapping(USER_PATH)
    public ResponseEntity<?> getUser(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            HttpServletRequest request
    ) {
        return responseMapper.map(userService.getUser(userId), request);
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @BadRequestErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "User's profile has been updated",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserUpdateDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "email": "example@info.com",
                                            "nickname": "user_12345",
                                            "photoBase64": "User photo",
                                            "bannedUntil": "2026-04-21 12:00:00",
                                            "bannedPermanently": false,
                                            "banReason": "Spam distribution",
                                            "roles": "USER",
                                            "locale": "de"
                                          },
                                          "message": "Your profile has been updated",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(USER_PATH)
    public ResponseEntity<?> updateUser(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User data for the update", required = true)
            @Validated @RequestBody UserUpdateDTO dto,

            @Parameter(
                    name = "If-Match",
                    description = "Entity version for optimistic locking",
                    example = "\"3\"",
                    required = true
            )
            @RequestHeader("If-Match") String ifMatch,

            HttpServletRequest request
    ) {
        return responseMapper.map(userService.updateUser(userId, dto, parserUtil.ifMatchParser(ifMatch)), request);
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "User account has been deleted",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "Your account has been deleted",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @DeleteMapping(USER_PATH)
    public ResponseEntity<?> deleteUser(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(
                    name = "If-Match",
                    description = "Entity version for optimistic locking",
                    example = "\"3\"",
                    required = true
            )
            @RequestHeader("If-Match") String ifMatch,

            HttpServletRequest request
    ) {
        return responseMapper.map(userService.deleteUser(userId, parserUtil.ifMatchParser(ifMatch)), request);
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @BadRequestErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "User's password has been changed",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "Your password has been changed",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(USER_PATH_RESET_PASSWORD)
    public ResponseEntity<?> resetPassword(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User data to change the password")
            @Validated @RequestBody UserResetPasswordDTO dto,

            @Parameter(
                    name = "If-Match",
                    description = "Entity version for optimistic locking",
                    example = "\"3\"",
                    required = true
            )
            @RequestHeader("If-Match") String ifMatch,

            HttpServletRequest request
    ) {
        return responseMapper.map(userService.resetPassword(userId, dto, parserUtil.ifMatchParser(ifMatch)), request);
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "User has been logged out",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "You have successfully logged out",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(USER_PATH_LOGOUT)
    public ResponseEntity<?> logout(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Refresh token to be deleted", required = true)
            @Validated @RequestBody RefreshTokenDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(userService.logout(userId, dto), request);
    }
}
