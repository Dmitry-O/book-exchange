package com.example.bookexchange.services;

import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;

public interface UserService {

    UserDTO getUser(Long userId);

    UserDTO createUser(UserCreateDTO dto);

    String updateUser(Long userId, UserDTO dto);

    String deleteUser(Long userId);
}
