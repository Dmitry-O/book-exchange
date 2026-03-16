package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class AuthController {

    private final UserService userService;

    public static final String AUTH_PATH = "/api/v1/auth";
    public static final String AUTH_PATH_REGISTER = AUTH_PATH + "/register";
    public static final String AUTH_PATH_LOGIN = AUTH_PATH + "/login";
    public static final String AUTH_PATH_REFRESH_TOKEN = AUTH_PATH + "/refresh_token";
    public static final String AUTH_PATH_CONFIRM_REGISTRATION = AUTH_PATH + "/verify";
    public static final String AUTH_PATH_FORGOT_PASSWORD = AUTH_PATH + "/forgot_password";
    public static final String AUTH_PATH_RESET_PASSWORD = AUTH_PATH + "/reset_password";
    public static final String AUTH_PATH_RESEND_CONFIRMATION_EMAIL = AUTH_PATH + "/resend_confirmation_email";
    public static final String AUTH_PATH_INITIATE_DELETE_ACCOUNT = AUTH_PATH + "/initiate_delete_account";
    public static final String AUTH_PATH_DELETE_ACCOUNT = AUTH_PATH + "/delete_account";

    @PostMapping(AUTH_PATH_REGISTER)
    public ResponseEntity<ApiMessage> register(@Validated @RequestBody UserCreateDTO dto) {
        return ResponseEntity.ok(new ApiMessage(userService.createUser(dto)));
    }

    @PostMapping(AUTH_PATH_LOGIN)
    public AuthResponseDTO login(@Validated @RequestBody AuthRequestDTO requestDTO) {
        return userService.loginUser(requestDTO);
    }

    @PostMapping(AUTH_PATH_REFRESH_TOKEN)
    public ResponseEntity<ApiMessage> refreshAccessToken(@Validated @RequestBody AuthRefreshTokenDTO dto) {
        return ResponseEntity.ok(new ApiMessage(userService.refreshAccessToken(dto.getRefreshToken())));
    }

    @GetMapping(AUTH_PATH_CONFIRM_REGISTRATION)
    public ResponseEntity<ApiMessage> confirmEmail(@RequestParam String token) {
        return ResponseEntity.ok(new ApiMessage(userService.confirmRegistration(token)));
    }

    @PatchMapping(AUTH_PATH_FORGOT_PASSWORD)
    public ResponseEntity<ApiMessage> forgotPassword(@Validated @RequestBody UserForgotPasswordDTO dto) {
        return ResponseEntity.ok(new ApiMessage(userService.forgotPassword(dto)));
    }

    @PatchMapping(AUTH_PATH_RESET_PASSWORD)
    public ResponseEntity<ApiMessage> resetPassword(@RequestParam String token, @Validated @RequestBody UserResetForgottenPasswordDTO dto) {
        return ResponseEntity.ok(new ApiMessage(userService.resetForgottenPassword(token, dto)));
    }

    @PatchMapping(AUTH_PATH_RESEND_CONFIRMATION_EMAIL)
    public ResponseEntity<ApiMessage> resendEmailConfirmation(@Validated @RequestBody UserResendEmailConfirmationDTO dto) {
        return ResponseEntity.ok(new ApiMessage(userService.resendEmailConfirmation(dto)));
    }

    @PatchMapping(AUTH_PATH_INITIATE_DELETE_ACCOUNT)
    public ResponseEntity<ApiMessage> initiateDeleteAccount(@Validated @RequestBody UserInitiateDeleteAccountDTO dto) {
        return ResponseEntity.ok(new ApiMessage(userService.initiateDeleteAccount(dto)));
    }

    @PatchMapping(AUTH_PATH_DELETE_ACCOUNT)
    public ResponseEntity<ApiMessage> deleteAccount(@RequestParam String token) {
        return ResponseEntity.ok(new ApiMessage(userService.deleteAccount(token)));
    }
}
