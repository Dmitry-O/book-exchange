package com.example.bookexchange.mapper;

import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.models.User;

public class UserMapper {
    public static UserDTO fromEntity(User user) {
        UserDTO dto = new UserDTO();

        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setPhotoBase64(user.getPhotoBase64());

        return dto;
    }

    public static User toEntity(UserCreateDTO dto) {
        User user = new User();

        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname());
        user.setPhotoBase64(dto.getPhotoBase64());

        return user;
    }
}
