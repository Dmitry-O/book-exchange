package com.example.bookexchange.controllers;

import com.example.bookexchange.auth.api.AuthController;
import com.example.bookexchange.auth.dto.AuthRefreshTokenDTO;
import com.example.bookexchange.auth.dto.AuthLoginRequestDTO;
import com.example.bookexchange.auth.dto.AuthLoginResponseDTO;
import com.example.bookexchange.user.dto.UserCreateDTO;
import com.example.bookexchange.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class AuthControllerIT extends AbstractIT {

    @Autowired
    AuthController authController;

    @Autowired
    UserService userService;

    //TODO: Update this test
    @Rollback
    @Transactional
    @Test
    void registerUser() {
        UserCreateDTO userCreateDTO = UserCreateDTO
                .builder()
                .email("user0@test.com")
                .password("password")
                .nickname("user0123456789012345")
                .build();

        ResponseEntity<String> responseEntity = authController.register(userCreateDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
    }

    @Rollback
    @Transactional
    @Test
    void registerUserBadRequest() {

    }

    @Rollback
    @Transactional
    @Test
    void loginUser() {
        UserCreateDTO userCreateDTO = UserCreateDTO
                .builder()
                .email("user0@test.com")
                .password("password")
                .nickname("user0123456789012345")
                .build();

        userService.createUser(userCreateDTO);

        AuthLoginRequestDTO authLoginRequestDTO = AuthLoginRequestDTO
                .builder()
                .email(userCreateDTO.getEmail())
                .password("password")
                .build();

        AuthLoginResponseDTO authLoginResponseDTO = authController.login(authLoginRequestDTO);

        assertThat(authLoginResponseDTO.getAccessToken()).isNotNull();
        assertThat(authLoginResponseDTO.getRefreshToken()).isNotNull();
    }

    @Rollback
    @Transactional
    @Test
    void loginUserBadRequest() {

    }

    @Rollback
    @Transactional
    @Test
    void refreshAccessToken() {
        UserCreateDTO userCreateDTO = UserCreateDTO
                .builder()
                .email("user0@test.com")
                .password("password")
                .nickname("user0123456789012345")
                .build();

        userService.createUser(userCreateDTO);

        AuthLoginRequestDTO authLoginRequestDTO = AuthLoginRequestDTO
                .builder()
                .email(userCreateDTO.getEmail())
                .password("password")
                .build();

        AuthLoginResponseDTO authLoginResponseDTO = userService.loginUser(authLoginRequestDTO);

        AuthRefreshTokenDTO authRefreshTokenDTO = AuthRefreshTokenDTO
                .builder()
                .refreshToken(authLoginResponseDTO.getRefreshToken())
                .build();

        String newAccessToken = authController.refreshAccessToken(authRefreshTokenDTO);

        assertThat(newAccessToken).isNotNull();
    }
}
