package com.example.bookexchange.services;

import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.dto.UserUpdateDTO;

public interface UserService {

    UserDTO getUser(Long userId);

    UserDTO createUser(UserCreateDTO dto);

    void updateUser(Long userId, UserUpdateDTO dto);

    void deleteUser(Long userId);
}
