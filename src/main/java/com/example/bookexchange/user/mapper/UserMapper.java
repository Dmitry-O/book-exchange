package com.example.bookexchange.user.mapper;

import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.dto.UserCreateDTO;
import com.example.bookexchange.user.dto.UserDTO;
import com.example.bookexchange.user.dto.UserUpdateDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface UserMapper {

    @Mapping(target = "password", source = "password")
    User userCreateDtoToUser(UserCreateDTO dto);

    @Mapping(target = "bannedUntil", source = "bannedUntil")
    @Mapping(target = "bannedPermanently", source = "bannedPermanently")
    @Mapping(target = "banReason", source = "banReason")
    UserDTO userToUserDto(User user);

    User userDtoToUser(UserDTO dto);

    @Mapping(target = "email", source = "dto.email")
    @Mapping(target = "nickname", source = "dto.nickname")
    @Mapping(target = "photoBase64", source = "dto.photoBase64")
    @Mapping(target = "locale", source = "dto.locale")
    void updateUserDtoToUser(UserUpdateDTO dto, @MappingTarget User user);
}
