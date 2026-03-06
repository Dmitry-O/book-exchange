package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.AuthRefreshTokenDTO;
import com.example.bookexchange.dto.AuthRequestDTO;
import com.example.bookexchange.dto.AuthResponseDTO;
import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.services.UserService;
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

        AuthRequestDTO authRequestDTO = AuthRequestDTO
                .builder()
                .email(userCreateDTO.getEmail())
                .password("password")
                .build();

        AuthResponseDTO authResponseDTO = authController.login(authRequestDTO);

        assertThat(authResponseDTO.getAccessToken()).isNotNull();
        assertThat(authResponseDTO.getRefreshToken()).isNotNull();
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

        AuthRequestDTO authRequestDTO = AuthRequestDTO
                .builder()
                .email(userCreateDTO.getEmail())
                .password("password")
                .build();

        AuthResponseDTO authResponseDTO = userService.loginUser(authRequestDTO);

        AuthRefreshTokenDTO authRefreshTokenDTO = AuthRefreshTokenDTO
                .builder()
                .refreshToken(authResponseDTO.getRefreshToken())
                .build();

        String newAccessToken = authController.refreshAccessToken(authRefreshTokenDTO);

        assertThat(newAccessToken).isNotNull();
    }
}
