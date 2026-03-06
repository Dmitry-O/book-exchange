package com.example.bookexchange.mappers;

import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "password", source = "password")
    User userDtoToUser(UserCreateDTO dto);

    @Mapping(target = "bannedUntil", source = "bannedUntil")
    @Mapping(target = "bannedPermanently", source = "bannedPermanently")
    @Mapping(target = "banReason", source = "banReason")
    UserDTO userToUserDto(User user);
}
