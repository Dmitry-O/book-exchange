package com.example.bookexchange.controllers;

import com.example.bookexchange.core.web.ResultResponseMapper;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ResultResponseMapper responseMapper;

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
    public ResponseEntity<?> register(@Validated @RequestBody UserCreateDTO dto, HttpServletRequest request) {
        return responseMapper.map(userService.createUser(dto), request);
    }

    @PostMapping(AUTH_PATH_LOGIN)
    public ResponseEntity<?> login(@Validated @RequestBody AuthRequestDTO requestDTO, HttpServletRequest request) {
        return responseMapper.map(userService.loginUser(requestDTO), request);
    }

    @PostMapping(AUTH_PATH_REFRESH_TOKEN)
    public ResponseEntity<?> refreshAccessToken(@Validated @RequestBody AuthRefreshTokenDTO dto, HttpServletRequest request) {
        return responseMapper.map(userService.refreshAccessToken(dto.getRefreshToken()), request);
    }

    @GetMapping(AUTH_PATH_CONFIRM_REGISTRATION)
    public ResponseEntity<?> confirmEmail(@RequestParam String token, HttpServletRequest request) {
        return responseMapper.map(userService.confirmRegistration(token), request);
    }

    @PatchMapping(AUTH_PATH_FORGOT_PASSWORD)
    public ResponseEntity<?> forgotPassword(@Validated @RequestBody UserForgotPasswordDTO dto, HttpServletRequest request) {
        return responseMapper.map(userService.forgotPassword(dto), request);
    }

    @PatchMapping(AUTH_PATH_RESET_PASSWORD)
    public ResponseEntity<?> resetPassword(
            @RequestParam String token,
            @Validated @RequestBody UserResetForgottenPasswordDTO dto,
            HttpServletRequest request
    ) {
        return responseMapper.map(userService.resetForgottenPassword(token, dto), request);
    }

    @PatchMapping(AUTH_PATH_RESEND_CONFIRMATION_EMAIL)
    public ResponseEntity<?> resendEmailConfirmation(
            @Validated @RequestBody UserResendEmailConfirmationDTO dto,
            HttpServletRequest request
    ) {
        return responseMapper.map(userService.resendEmailConfirmation(dto), request);
    }

    @PatchMapping(AUTH_PATH_INITIATE_DELETE_ACCOUNT)
    public ResponseEntity<?> initiateDeleteAccount(
            @Validated @RequestBody UserInitiateDeleteAccountDTO dto,
            HttpServletRequest request
    ) {
        return responseMapper.map(userService.initiateDeleteAccount(dto), request);
    }

    @PatchMapping(AUTH_PATH_DELETE_ACCOUNT)
    public ResponseEntity<?> deleteAccount(@RequestParam String token, HttpServletRequest request) {
        return responseMapper.map(userService.deleteAccount(token), request);
    }
}
