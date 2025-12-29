package com.example.bookexchange.controllers;

import com.example.bookexchange.config.TestUserConfig;
import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.mappers.UserMapper;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.util.UserUtil;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestUserConfig.class)
class UserControllerIT {

    @Autowired
    UserController userController;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserMapper userMapper;

    @Autowired
    UserUtil userUtil;

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
        userUtil.createUser(null);

        User user = userRepository.findAll().get(0);
        UserDTO userDTO = userMapper.userToUserDto(user);

        final String userNickname = "user2";

        userDTO.setId(null);
        userDTO.setNickname(userNickname);

        ResponseEntity responseEntity = userController.updateUser(user.getId(), userDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        User updatedUser = userRepository.findById(user.getId()).get();

        assertThat(updatedUser.getNickname()).isEqualTo(userNickname);
    }

    @Test
    void updateUserNotFound() {
        assertThrows(EntityNotFoundException.class, () -> userController.updateUser(System.nanoTime(), UserDTO.builder().build()));
    }

    @Rollback
    @Transactional
    @Test
    void updateUserAlreadyExists() {
        userUtil.createUser(1);
        userUtil.createUser(2);

        User user = userRepository.findAll().get(1);
        UserDTO userDTO = userMapper.userToUserDto(user);

        final String userNickname = "user1";

        userDTO.setId(null);
        userDTO.setNickname(userNickname);

        assertThrows(EntityExistsException.class, () -> userController.updateUser(user.getId(), userDTO));
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