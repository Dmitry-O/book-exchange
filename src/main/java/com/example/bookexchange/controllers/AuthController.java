package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class AuthController {

    private final UserService userService;

    public static final String AUTH_PATH ="/api/v1/auth";
    public static final String AUTH_PATH_REGISTER = AUTH_PATH + "/register";
    public static final String AUTH_PATH_LOGIN = AUTH_PATH + "/login";
    public static final String AUTH_PATH_REFRESH_TOKEN = AUTH_PATH + "/refresh_token";
    public static final String AUTH_PATH_CONFIRM_REGISTRATION = AUTH_PATH + "/confirm_registration";
    public static final String AUTH_PATH_FORGOT_PASSWORD = AUTH_PATH + "/forgot_password";
    public static final String AUTH_PATH_RESET_PASSWORD = AUTH_PATH + "/reset_password";
    public static final String AUTH_PATH_RESEND_CONFIRMATION_EMAIL = AUTH_PATH + "/resend_confirmation_email";

    @PostMapping(AUTH_PATH_REGISTER)
    public VerificationTokenDTO register(@Validated @RequestBody UserCreateDTO dto) {
        return userService.createUser(dto);
    }

    @PostMapping(AUTH_PATH_LOGIN)
    public AuthResponseDTO login(@Validated @RequestBody AuthRequestDTO requestDTO) {
        return userService.loginUser(requestDTO);
    }

    @PostMapping(AUTH_PATH_REFRESH_TOKEN)
    public String refreshAccessToken(@Validated @RequestBody AuthRefreshTokenDTO dto) {
        return userService.refreshAccessToken(dto.getRefreshToken());
    }

    @PatchMapping(AUTH_PATH_CONFIRM_REGISTRATION)
    public ResponseEntity<String> confirmEmail(@RequestParam String token) {
        userService.confirmRegistration(token);

        return new ResponseEntity<>("Die Registrierung wurde vollständig abgeschlossen", HttpStatus.OK);
    }

    @PatchMapping(AUTH_PATH_FORGOT_PASSWORD)
    public VerificationTokenDTO forgotPassword(@Validated @RequestBody UserForgotPasswordDTO dto) {
        return userService.forgotPassword(dto);
    }

    @PatchMapping(AUTH_PATH_RESET_PASSWORD)
    public ResponseEntity<String> resetPassword(@Validated @RequestBody UserResetForgottenPasswordDTO dto) {
        userService.resetForgottenPassword(dto);

        return new ResponseEntity<>("Ihr Passwort wurde erfolgreich geändert", HttpStatus.OK);
    }

    @PatchMapping(AUTH_PATH_RESEND_CONFIRMATION_EMAIL)
    public VerificationTokenDTO resendEmailConfirmation(@Validated @RequestBody UserResendEmailConfirmationDTO dto) {
        return userService.resendEmailConfirmation(dto);
    }
}
