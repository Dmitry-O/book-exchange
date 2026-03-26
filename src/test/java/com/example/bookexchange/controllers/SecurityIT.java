package com.example.bookexchange.controllers;

import com.example.bookexchange.user.dto.UserUpdateDTO;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.security.auth.JwtService;
import com.example.bookexchange.user.api.UserController;
import com.example.bookexchange.util.UserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
public class SecurityIT extends AbstractIT {

    @Autowired
    UserUtil userUtil;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtService jwtService;

    @Autowired
    ObjectMapper objectMapper;

    MockMvc mockMvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Rollback
    @Transactional
    @Test
    void updateUserAuthorized() throws Exception {
        final String userNickname = "user0123456789012345";

        userUtil.createUser(null);

        User user = userRepository.findAll().getFirst();

        String token = jwtService.generateToken(user);

        UserUpdateDTO dto = UserUpdateDTO.builder()
                .email(user.getEmail())

                .nickname(userNickname)
                .build();

        mockMvc.perform(patch(UserController.USER_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());
    }

    @Rollback
    @Transactional
    @Test
    void updateUserWithoutToken() throws Exception {
        final String userNickname = "user0123456789012345";

        userUtil.createUser(null);

        User user = userRepository.findAll().getFirst();

        UserUpdateDTO dto = UserUpdateDTO.builder()
                .email(user.getEmail())
                .nickname(userNickname)
                .build();

        mockMvc.perform(patch(UserController.USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Rollback
    @Transactional
    @Test
    void updateUserWithInvalidToken() throws Exception {
        final String userNickname = "user0123456789012345";

        userUtil.createUser(null);

        User user = userRepository.findAll().getFirst();

        UserUpdateDTO dto = UserUpdateDTO.builder()
                .email(user.getEmail())

                .nickname(userNickname)
                .build();

        mockMvc.perform(patch(UserController.USER_PATH)
                        .header("Authorization", "Bearer test_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }
}
