package com.example.bookexchange.auth.api;

import com.example.bookexchange.auth.dto.AuthLoginRequestDTO;
import com.example.bookexchange.auth.dto.AuthLoginResponseDTO;
import com.example.bookexchange.auth.dto.AuthRefreshTokenDTO;
import com.example.bookexchange.auth.dto.VerificationTokenTypeDTO;
import com.example.bookexchange.auth.dto.VerificationTokenValidationDTO;
import com.example.bookexchange.auth.service.AuthService;
import com.example.bookexchange.common.swagger.error_response.BadRequestErrorResponse;
import com.example.bookexchange.common.swagger.error_response.ConflictErrorResponse;
import com.example.bookexchange.common.swagger.error_response.ForbiddenErrorResponse;
import com.example.bookexchange.common.swagger.error_response.NotFoundErrorResponse;
import com.example.bookexchange.common.web.ApiResponse;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.user.dto.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication")
@RestController
@AllArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final ResultResponseMapper responseMapper;

    @BadRequestErrorResponse
    @ForbiddenErrorResponse
    @ConflictErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "User's account has been created",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": null,
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid email address or password",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": false,
                                          "data": null,
                                          "message": null,
                                          "error": {
                                            "status": 401,
                                            "error": "AUTH_INVALID_CREDENTIALS",
                                            "message": "Incorrect email address or password",
                                            "path": "/api/v1/auth/login"
                                          }
                                        }
                                """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
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
                                          "message": null,
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "A new access token has been generated",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Verification token is valid for the requested action",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = VerificationTokenValidationDTO.class)
            )
    )
    @GetMapping(AuthPaths.AUTH_PATH_VALIDATE_TOKEN)
    public ResponseEntity<?> validateVerificationToken(
            @Parameter(description = "Verification token from the email action link")
            @RequestParam @NotBlank @Size(max = 128) String token,

            @Parameter(description = "Action for which the token must be valid")
            @RequestParam VerificationTokenTypeDTO tokenType,

            HttpServletRequest request
    ) {
        return responseMapper.map(authService.validateVerificationToken(token, tokenType), request);
    }

    @BadRequestErrorResponse
    @NotFoundErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User's email address has been confirmed",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "Your email address has been confirmed. Welcome to Book Exchange!",
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

    @BadRequestErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Returns the same neutral response regardless of account existence or eligibility",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "If an account with this email address exists and the action is available, we have sent an email to your inbox.",
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User's password has been reset",
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Returns the same neutral response regardless of account existence or eligibility",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "If an account with this email address exists and the action is available, we have sent an email to your inbox.",
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

    @BadRequestErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Returns the same neutral response regardless of account existence or eligibility",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "If an account with this email address exists and the action is available, we have sent an email to your inbox.",
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User's account has been deleted",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
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
