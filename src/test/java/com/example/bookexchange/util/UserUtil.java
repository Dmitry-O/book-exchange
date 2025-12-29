package com.example.bookexchange.util;

import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.services.UserService;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class UserUtil {

    private final UserService userService;

    public Long createUser(Integer userNumberParam) {
        Integer userNumber = Optional.ofNullable(userNumberParam).orElse(1);

        UserCreateDTO userCreateDTO = UserCreateDTO.builder()
                .email("user" + userNumber + "@test.com")
                .nickname("user" + userNumber)
                .photoBase64("photo.jpg")
                .build();

        UserDTO savedUser = userService.createUser(userCreateDTO);

        return savedUser.getId();
    }

    public void deleteUser(Long userId) {
        userService.deleteUser(userId);
    }
}
