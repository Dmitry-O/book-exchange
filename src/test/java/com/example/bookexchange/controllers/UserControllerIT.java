package com.example.bookexchange.controllers;

import com.example.bookexchange.config.TestUserConfig;
import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.dto.UserUpdateDTO;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.util.UserUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestUserConfig.class)
class UserControllerIT {

    @Autowired
    UserController userController;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserUtil userUtil;

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    ObjectMapper objectMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Rollback
    @Transactional
    @Test
    void getUser() {
        userUtil.createUser(null);

        User user = userRepository.findAll().get(0);

        UserDTO userDTO = userController.getUser(user.getId());

        assertThat(userDTO).isNotNull();
    }

    @Test
    void getUserNotFound() {
        assertThrows(EntityNotFoundException.class, () -> userController.getUser(System.nanoTime()));
    }

    @Rollback
    @Transactional
    @Test
    void createUser() {
        UserCreateDTO userCreateDTO = UserCreateDTO.builder()
                .email("user1@test.com")
                .nickname("user1")
                .photoBase64("photo.jpg")
                .build();

        ResponseEntity responseEntity = userController.createUser(userCreateDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
        assertThat(responseEntity.getHeaders().getLocation()).isNotNull();

        String[] location = responseEntity.getHeaders().getLocation().getPath().split("/");
        Long savedId = Long.valueOf(location[4]);

        User user = userRepository.findById(savedId).get();

        assertThat(user).isNotNull();
    }

    @Rollback
    @Transactional
    @Test
    void createUserBadRequest() throws Exception {
        UserCreateDTO userCreateDTO = UserCreateDTO.builder().build();

        MvcResult mvcResult = mockMvc.perform(post(UserController.USER_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateDTO))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()", is(4)))
                .andReturn();

        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Rollback
    @Transactional
    @Test
    void createUserAlreadyExists() {
        userUtil.createUser(null);

        UserCreateDTO userCreateDTO = UserCreateDTO.builder()
                .email("user1@test.com")
                .nickname("user1")
                .photoBase64("photo.jpg")
                .build();

        assertThrows(EntityExistsException.class, () -> userController.createUser(userCreateDTO));
    }

    @Rollback
    @Transactional
    @Test
    void updateUser() {
        final String userNickname = "user2";

        userUtil.createUser(null);

        User user = userRepository.findAll().get(0);

        UserUpdateDTO userUpdateDTO = UserUpdateDTO.builder()
                .email(user.getEmail())
                .nickname(userNickname)
                .photoBase64(user.getPhotoBase64())
                .build();

        ResponseEntity responseEntity = userController.updateUser(user.getId(), userUpdateDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        User updatedUser = userRepository.findById(user.getId()).get();

        assertThat(updatedUser.getNickname()).isEqualTo(userNickname);
    }

    @Test
    void updateUserNotFound() {
        assertThrows(EntityNotFoundException.class, () -> userController.updateUser(System.nanoTime(), UserUpdateDTO.builder().build()));
    }

    @Rollback
    @Transactional
    @Test
    void updateUserAlreadyExists() {
        final String userNickname = "user1";

        userUtil.createUser(1);
        userUtil.createUser(2);

        User user = userRepository.findAll().get(1);

        UserUpdateDTO userUpdateDTO = UserUpdateDTO.builder()
                .email(user.getEmail())
                .nickname(userNickname)
                .photoBase64(user.getPhotoBase64())
                .build();

        assertThrows(EntityExistsException.class, () -> userController.updateUser(user.getId(), userUpdateDTO));
    }

    @Rollback
    @Transactional
    @Test
    void updateUserBadRequest() throws Exception {
        final String userNickname = "user01234567890123456789";

        userUtil.createUser(null);

        User user = userRepository.findAll().get(0);

        UserUpdateDTO userUpdateDTO = UserUpdateDTO.builder()
                .email(user.getEmail())
                .nickname(userNickname)
                .photoBase64(user.getPhotoBase64())
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(UserController.USER_PATH_USER_ID, user.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userUpdateDTO))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()", is(1)))
                .andReturn();

        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Rollback
    @Transactional
    @Test
    void deleteUser() {
        userUtil.createUser(null);

        User user = userRepository.findAll().get(0);

        ResponseEntity responseEntity = userController.deleteUser(user.getId());

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));
        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    @Test
    void deleteUserNotFound() {
        assertThrows(EntityNotFoundException.class, () -> userController.deleteUser(System.nanoTime()));
    }
}