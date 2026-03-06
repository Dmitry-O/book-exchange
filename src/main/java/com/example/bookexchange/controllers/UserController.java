package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.dto.UserResetPasswordDTO;
import com.example.bookexchange.dto.UserUpdateDTO;
import com.example.bookexchange.models.User;
import com.example.bookexchange.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class UserController {

    private UserService userService;
    public static final String USER_PATH = "/api/v1/user";
    public static final String USER_PATH_RESET_PASSWORD = USER_PATH + "/reset_password";

    @GetMapping(USER_PATH)
    public UserDTO getUser(@AuthenticationPrincipal User user) {
        return userService.getUser(user.getId());
    }

    @PatchMapping(USER_PATH)
    public ResponseEntity<String> updateUser(@AuthenticationPrincipal User user, @Validated @RequestBody UserUpdateDTO dto) {
        userService.updateUser(user.getId(), dto);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(USER_PATH)
    public ResponseEntity<String> deleteUser(@AuthenticationPrincipal User user) {
        userService.deleteUser(user.getId());

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping(USER_PATH_RESET_PASSWORD)
    public ResponseEntity<String> resetPassword(@AuthenticationPrincipal User user, @Validated @RequestBody UserResetPasswordDTO dto) {
        userService.resetPassword(user, dto);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
