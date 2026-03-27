package com.example.bookexchange.user.mapper;

import com.example.bookexchange.common.mapper.TemporalMapper;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.dto.UserCreateDTO;
import com.example.bookexchange.user.dto.SupportedLocalesDTO;
import com.example.bookexchange.user.dto.UserDTO;
import com.example.bookexchange.user.dto.UserUpdateDTO;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        uses = TemporalMapper.class
)
public interface UserMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "nickname", source = "nickname")
    @Mapping(target = "locale", source = "locale", qualifiedByName = "normalizeLocale")
    User userCreateDtoToUser(UserCreateDTO dto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "nickname", source = "nickname")
    @Mapping(target = "photoBase64", source = "photoBase64")
    @Mapping(target = "bannedUntil", source = "bannedUntil")
    @Mapping(target = "bannedPermanently", source = "bannedPermanently")
    @Mapping(target = "banReason", source = "banReason")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "locale", source = "locale")
    UserDTO userToUserDto(User user);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "email", source = "dto.email")
    @Mapping(target = "nickname", source = "dto.nickname")
    @Mapping(target = "photoBase64", source = "dto.photoBase64")
    @Mapping(target = "locale", source = "dto.locale", qualifiedByName = "normalizeLocale")
    void updateUserDtoToUser(UserUpdateDTO dto, @MappingTarget User user);

    @Named("normalizeLocale")
    default String normalizeLocale(String locale) {
        return locale == null ? null : locale.toLowerCase();
    }

    default SupportedLocalesDTO map(String locale) {
        return locale == null ? null : SupportedLocalesDTO.fromValue(locale);
    }
}
