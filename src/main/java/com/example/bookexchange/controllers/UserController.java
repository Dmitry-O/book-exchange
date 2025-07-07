package com.example.bookexchange.controllers;

import com.example.bookexchange.models.User;
import com.example.bookexchange.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("api/v1/user")
public class UserController {
    private UserService userService;

    @RequestMapping(value = "/{userId}", method = RequestMethod.GET)
    public User getUser(@PathVariable("userId") Long userId) {
        return userService.getUser(userId);
    }

    @RequestMapping(method = RequestMethod.POST)
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @RequestMapping(value = "/{userId}", method = RequestMethod.PUT)
    public User updateUser(@PathVariable("userId") Long userId, @RequestBody User user) {
        return userService.updateUser(userId, user);
    }

    @RequestMapping(value = "/{userId}", method = RequestMethod.DELETE)
    public String deleteUser(@PathVariable("userId") Long userId) {
        return userService.deleteUser(userId);
    }
}
