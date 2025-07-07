package com.example.bookexchange.services;

import com.example.bookexchange.models.User;

public interface UserService {
    User getUser(Long userId);

    User createUser(User user);

    User updateUser(Long userId, User user);

    String deleteUser(Long userId);
}
