package com.example.bookexchange.mappers;

import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.models.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    User userDtoToUser(UserCreateDTO dto);
    UserDTO userToUserDto(User user);
}
