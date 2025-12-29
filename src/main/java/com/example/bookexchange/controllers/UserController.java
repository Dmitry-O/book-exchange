package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class UserController {

    public static final String USER_PATH = "/api/v1/user";
    public static final String USER_PATH_USER_ID = USER_PATH + "/{userId}";

    private UserService userService;

    @GetMapping(USER_PATH_USER_ID)
    public UserDTO getUser(@PathVariable Long userId) {
        UserDTO userDTO = userService.getUser(userId);

        return userDTO;
    }

    @PostMapping(USER_PATH)
    public ResponseEntity createUser(@RequestBody UserCreateDTO dto) {
        UserDTO savedUser = userService.createUser(dto);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, USER_PATH + "/" + savedUser.getId());

        return new ResponseEntity(headers, HttpStatus.CREATED);
    }

    @PatchMapping(USER_PATH_USER_ID)
    public ResponseEntity updateUser(@PathVariable Long userId, @RequestBody UserDTO dto) {
        userService.updateUser(userId, dto);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(USER_PATH_USER_ID)
    public ResponseEntity deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
}
