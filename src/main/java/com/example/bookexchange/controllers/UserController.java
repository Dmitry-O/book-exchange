package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.mappers.UserMapper;
import com.example.bookexchange.models.User;
import com.example.bookexchange.services.UserService;
import com.example.bookexchange.util.ParserUtil;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class UserController {

    private final UserMapper userMapper;
    private UserService userService;
    private ParserUtil parserUtil;

    public static final String USER_PATH = "/api/v1/user";
    public static final String USER_PATH_RESET_PASSWORD = USER_PATH + "/reset_password";
    public static final String USER_PATH_LOGOUT = USER_PATH + "/logout";

    @GetMapping(USER_PATH)
    public ResponseEntity<UserDTO> getUser(@CurrentUser Long userId) {
        User user = userService.getUser(userId);

        return ResponseEntity
                .ok()
                .eTag("\"" + user.getVersion() + "\"")
                .body(userMapper.userToUserDto(user));
    }

    @PatchMapping(USER_PATH)
    public ResponseEntity<ApiMessage> updateUser(
            @CurrentUser Long userId,
            @Validated @RequestBody UserUpdateDTO dto,
            @RequestHeader("If-Match") String ifMatch
    ) {
        return ResponseEntity.ok(new ApiMessage(userService.updateUser(userId, dto, parserUtil.ifMatchParser(ifMatch))));
    }

    @DeleteMapping(USER_PATH)
    public ResponseEntity<ApiMessage> deleteUser(@CurrentUser Long userId, @RequestHeader("If-Match") String ifMatch) {
        return ResponseEntity.ok(new ApiMessage(userService.deleteUser(userId, false, parserUtil.ifMatchParser(ifMatch))));
    }

    @PatchMapping(USER_PATH_RESET_PASSWORD)
    public ResponseEntity<ApiMessage> resetPassword(
            @CurrentUser Long userId,
            @Validated @RequestBody UserResetPasswordDTO dto,
            @RequestHeader("If-Match") String ifMatch
    ) {
        return ResponseEntity.ok(new ApiMessage(userService.resetPassword(userId, dto, parserUtil.ifMatchParser(ifMatch))));
    }

    @PatchMapping(USER_PATH_LOGOUT)
    public ResponseEntity<ApiMessage> logout(@CurrentUser Long userId, @Validated @RequestBody RefreshTokenDTO dto) {
        return ResponseEntity.ok(new ApiMessage(userService.logout(userId, dto)));
    }
}
