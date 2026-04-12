package com.example.bookexchange.user.api;

import com.example.bookexchange.auth.dto.RefreshTokenDTO;
import com.example.bookexchange.security.auth.CurrentUser;
import com.example.bookexchange.common.swagger.error_response.BadRequestErrorResponse;
import com.example.bookexchange.common.swagger.error_response.ConflictErrorResponse;
import com.example.bookexchange.common.swagger.error_response.NotFoundErrorResponse;
import com.example.bookexchange.common.swagger.error_response.UnauthorizedErrorResponse;
import com.example.bookexchange.common.web.ApiResponse;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.user.service.UserService;
import com.example.bookexchange.user.dto.UserDTO;
import com.example.bookexchange.user.dto.UserResetPasswordDTO;
import com.example.bookexchange.user.dto.UserUpdateDTO;
import com.example.bookexchange.common.util.ParserUtil;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
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
    @GetMapping(UserPaths.USER_PATH)
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User's profile has been updated",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "email": "example@info.com",
                                            "nickname": "user_12345",
                                            "photoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/profile_photo_1712582410000.jpg",
                                            "bannedUntil": "2026-04-21T12:00:00Z",
                                            "bannedPermanently": false,
                                            "banReason": "Spam distribution",
                                            "roles": ["USER"],
                                            "locale": "de"
                                          },
                                          "message": "Your profile has been updated",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(UserPaths.USER_PATH)
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
    @BadRequestErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User profile photo has been deleted",
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
    @DeleteMapping(UserPaths.USER_PATH_PHOTO)
    public ResponseEntity<?> deleteUserPhoto(
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
        return responseMapper.map(userService.deleteUserPhoto(userId, parserUtil.ifMatchParser(ifMatch)), request);
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
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
    @DeleteMapping(UserPaths.USER_PATH)
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
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
    @PatchMapping(UserPaths.USER_PATH_RESET_PASSWORD)
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
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
    @PatchMapping(UserPaths.USER_PATH_LOGOUT)
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
