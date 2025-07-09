package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("api/v1/user")
public class UserController {

    private UserService userService;

    @GetMapping("/{userId}")
    public UserDTO getUser(@PathVariable("userId") Long userId) {
        return userService.getUser(userId);
    }

    @PostMapping()
    public UserDTO createUser(@RequestBody UserCreateDTO dto) {
        return userService.createUser(dto);
    }

    @PatchMapping("/{userId}")
    public String updateUser(@PathVariable("userId") Long userId, @RequestBody UserDTO dto) {
        return userService.updateUser(userId, dto);
    }

    @DeleteMapping("/{userId}")
    public String deleteUser(@PathVariable("userId") Long userId) {
        return userService.deleteUser(userId);
    }
}
