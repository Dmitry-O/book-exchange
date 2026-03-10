package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.dto.ApiMessage;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.dto.UserResetPasswordDTO;
import com.example.bookexchange.dto.UserUpdateDTO;
import com.example.bookexchange.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class UserController {

    private UserService userService;
    public static final String USER_PATH = "/api/v1/user";
    public static final String USER_PATH_RESET_PASSWORD = USER_PATH + "/reset_password";

    @GetMapping(USER_PATH)
    public UserDTO getUser(@CurrentUser Long userId) {
        return userService.getUser(userId);
    }

    @PatchMapping(USER_PATH)
    public ResponseEntity<ApiMessage> updateUser(@CurrentUser Long userId, @Validated @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(new ApiMessage(userService.updateUser(userId, dto)));
    }

    @DeleteMapping(USER_PATH)
    public ResponseEntity<ApiMessage> deleteUser(@CurrentUser Long userId) {
        return ResponseEntity.ok(new ApiMessage(userService.deleteUser(userId)));
    }

    @PatchMapping(USER_PATH_RESET_PASSWORD)
    public ResponseEntity<ApiMessage> resetPassword(@CurrentUser Long userId, @Validated @RequestBody UserResetPasswordDTO dto) {
        return ResponseEntity.ok(new ApiMessage(userService.resetPassword(userId, dto)));
    }
}
