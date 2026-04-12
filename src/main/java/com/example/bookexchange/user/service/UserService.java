package com.example.bookexchange.user.service;

import com.example.bookexchange.auth.dto.RefreshTokenDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.user.dto.*;

public interface UserService {

    Result<UserDTO> getUser(Long userId);

    Result<UserDTO> updateUser(Long userId, UserUpdateDTO dto, Long version);

    Result<UserDTO> deleteUserPhoto(Long userId, Long version);

    Result<Void> deleteUser(Long userId, Long version);

    Result<Void> resetPassword(Long userId, UserResetPasswordDTO dto, Long version);

    Result<Void> logout(Long userId, RefreshTokenDTO token);
}
