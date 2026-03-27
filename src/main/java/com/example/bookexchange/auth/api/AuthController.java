package com.example.bookexchange.auth.api;

import com.example.bookexchange.auth.dto.AuthLoginRequestDTO;
import com.example.bookexchange.auth.dto.AuthLoginResponseDTO;
import com.example.bookexchange.auth.dto.AuthRefreshTokenDTO;
import com.example.bookexchange.auth.service.AuthService;
import com.example.bookexchange.common.swagger.error_response.BadRequestErrorResponse;
import com.example.bookexchange.common.swagger.error_response.ConflictErrorResponse;
import com.example.bookexchange.common.swagger.error_response.ForbiddenErrorResponse;
import com.example.bookexchange.common.swagger.error_response.NotFoundErrorResponse;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.user.dto.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication")
@RestController
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ResultResponseMapper responseMapper;

    @BadRequestErrorResponse
    @ForbiddenErrorResponse
    @ConflictErrorResponse
    @ApiResponse(
            responseCode = "201",
            description = "User's account has been created",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = com.example.bookexchange.common.web.ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "Your account has been successfully registered. Please confirm your email address now.",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PostMapping(AuthPaths.AUTH_PATH_REGISTER)
    public ResponseEntity<?> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User's registration data",
                    required = true
            )
            @Validated @RequestBody UserCreateDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(authService.createUser(dto), request);
    }

    @BadRequestErrorResponse
    @ForbiddenErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "User has been logged in",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthLoginResponseDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "accessToken": "eyJhbGciOiJIUzUx...",
                                            "refreshToken": "3bc88331-60c0-467e-b4fa-e8958421ab68"
                                          },
                                          "message": "Your account has been successfully registered. Please confirm your email address now.",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PostMapping(AuthPaths.AUTH_PATH_LOGIN)
    public ResponseEntity<?> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User's login data",
                    required = true
            )
            @Validated @RequestBody AuthLoginRequestDTO requestDTO,

            HttpServletRequest request
    ) {
        return responseMapper.map(authService.loginUser(requestDTO), request);
    }

    @BadRequestErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "201",
            description = "A new access token has been generated",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = com.example.bookexchange.common.web.ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": "eyJhbGciOiJIUzUx...",
                                          "message": null,
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PostMapping(AuthPaths.AUTH_PATH_REFRESH_TOKEN)
    public ResponseEntity<?> refreshAccessToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refresh token for a new access token generation",
                    required = true
            )
            @Validated @RequestBody AuthRefreshTokenDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(authService.refreshAccessToken(dto.getRefreshToken()), request);
    }

    @BadRequestErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "User's email address has been confirmed",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = com.example.bookexchange.common.web.ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "The registration process has been fully completed",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @GetMapping(AuthPaths.AUTH_PATH_CONFIRM_REGISTRATION)
    public ResponseEntity<?> confirmEmail(
            @Parameter(description = "E-Mail verification token", example = "eyJhbGciOiJIUzUx...")
            @RequestParam String token,

            HttpServletRequest request
    ) {
        return responseMapper.map(authService.confirmRegistration(token), request);
    }

    @ForbiddenErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "An E-Mail has been sent to reset user's password",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = com.example.bookexchange.common.web.ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "We have sent you instructions on how to reset your password to your email address",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AuthPaths.AUTH_PATH_FORGOT_PASSWORD)
    public ResponseEntity<?> forgotPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User's E-Mail address to reset the forgotten password",
                    required = true
            )
            @Validated @RequestBody UserForgotPasswordDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(authService.forgotPassword(dto), request);
    }

    @BadRequestErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "User's password has been reset",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = com.example.bookexchange.common.web.ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "We have sent you instructions on how to reset your password to your email address",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AuthPaths.AUTH_PATH_RESET_PASSWORD)
    public ResponseEntity<?> resetPassword(
            @Parameter(description = "Reset-password token", example = "eyJhbGciOiJIUzUx...")
            @RequestParam String token,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User's new password",
                    required = true
            )
            @Validated @RequestBody UserResetForgottenPasswordDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(authService.resetForgottenPassword(token, dto), request);
    }

    @BadRequestErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "An additional confirmation has been sent to verify the E-Mail address",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = com.example.bookexchange.common.web.ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "We have emailed you because you first need to confirm your email address. Please check your inbox and follow the instructions.",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AuthPaths.AUTH_PATH_RESEND_CONFIRMATION_EMAIL)
    public ResponseEntity<?> resendEmailConfirmation(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User's E-Mail address to resend the E-mail confirmation",
                    required = true
            )
            @Validated @RequestBody UserResendEmailConfirmationDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(authService.resendEmailConfirmation(dto), request);
    }

    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "An E-Mail has been sent to delete user's account",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = com.example.bookexchange.common.web.ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "We have sent instructions on how to delete your account to your email address",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AuthPaths.AUTH_PATH_INITIATE_DELETE_ACCOUNT)
    public ResponseEntity<?> initiateDeleteAccount(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                        User's E-Mail address to initiate their account deletion when they are banned
                        and therefore don't have access to their profile to initiate the normal account deletion
                    """,
                    required = true
            )
            @Validated @RequestBody UserInitiateDeleteAccountDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(authService.initiateDeleteAccount(dto), request);
    }

    @ConflictErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "User's account has been deleted",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = com.example.bookexchange.common.web.ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "Your profile has been deleted",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AuthPaths.AUTH_PATH_DELETE_ACCOUNT)
    public ResponseEntity<?> deleteAccount(
            @Parameter(
                    description = "Token for deleting user's account when the user doesn't have access to their account",
                    example = "eyJhbGciOiJIUzUx..."
            )
            @RequestParam String token,

            HttpServletRequest request
    ) {
        return responseMapper.map(authService.deleteAccount(token), request);
    }
}
