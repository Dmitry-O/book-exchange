package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class UserController {

    public static final String USER_PATH = "/api/v1/user";
    public static final String USER_PATH_USER_ID = USER_PATH + "/{userId}";

    private UserService userService;

    @GetMapping(USER_PATH_USER_ID)
    public UserDTO getUser(@PathVariable("userId") Long userId) {
        return userService.getUser(userId);
    }

    @PostMapping(USER_PATH)
    public UserDTO createUser(@RequestBody UserCreateDTO dto) {
        return userService.createUser(dto);
    }

    @PatchMapping(USER_PATH_USER_ID)
    public String updateUser(@PathVariable("userId") Long userId, @RequestBody UserDTO dto) {
        return userService.updateUser(userId, dto);
    }

    @DeleteMapping(USER_PATH_USER_ID)
    public String deleteUser(@PathVariable("userId") Long userId) {
        return userService.deleteUser(userId);
    }
}
