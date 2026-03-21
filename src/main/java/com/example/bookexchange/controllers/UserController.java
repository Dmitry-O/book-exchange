package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.core.web.ResultResponseMapper;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.services.UserService;
import com.example.bookexchange.util.ParserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserService userService;
    private final ParserUtil parserUtil;
    private final ResultResponseMapper responseMapper;

    public static final String USER_PATH = "/api/v1/user";
    public static final String USER_PATH_RESET_PASSWORD = USER_PATH + "/reset_password";
    public static final String USER_PATH_LOGOUT = USER_PATH + "/logout";

    @GetMapping(USER_PATH)
    public ResponseEntity<?> getUser(@CurrentUser Long userId, HttpServletRequest request) {
        return responseMapper.map(userService.getUser(userId), request);
    }

    @PatchMapping(USER_PATH)
    public ResponseEntity<?> updateUser(
            @CurrentUser Long userId,
            @Validated @RequestBody UserUpdateDTO dto,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(userService.updateUser(userId, dto, parserUtil.ifMatchParser(ifMatch)), request);
    }

    @DeleteMapping(USER_PATH)
    public ResponseEntity<?> deleteUser(
            @CurrentUser Long userId,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(userService.deleteUser(userId, false, parserUtil.ifMatchParser(ifMatch)), request);
    }

    @PatchMapping(USER_PATH_RESET_PASSWORD)
    public ResponseEntity<?> resetPassword(
            @CurrentUser Long userId,
            @Validated @RequestBody UserResetPasswordDTO dto,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(userService.resetPassword(userId, dto, parserUtil.ifMatchParser(ifMatch)), request);
    }

    @PatchMapping(USER_PATH_LOGOUT)
    public ResponseEntity<?> logout(
            @CurrentUser Long userId,
            @Validated @RequestBody RefreshTokenDTO dto,
            HttpServletRequest request
    ) {
        return responseMapper.map(userService.logout(userId, dto), request);
    }
}
