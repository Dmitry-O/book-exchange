package com.example.bookexchange.services;

import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.dto.*;

public interface UserService {

    Result<UserDTO> getUser(Long userId);

    Result<Void> createUser(UserCreateDTO dto);

    Result<UserDTO> updateUser(Long userId, UserUpdateDTO dto, Long version);

    Result<Void> deleteUser(Long userId, Long version);

    Result<AuthResponseDTO> loginUser(AuthRequestDTO requestDTO);

    Result<String> refreshAccessToken(String token);

    Result<Void> resetPassword(Long userId, UserResetPasswordDTO dto, Long version);

    Result<Void> confirmRegistration(String token);

    Result<Void> forgotPassword(UserForgotPasswordDTO dto);

    Result<Void> resetForgottenPassword(String token, UserResetForgottenPasswordDTO dto);

    Result<Void> resendEmailConfirmation(UserResendEmailConfirmationDTO dto);

    Result<Void> initiateDeleteAccount(UserInitiateDeleteAccountDTO dto);

    Result<Void> deleteAccount(String token);

    Result<Void> logout(Long userId, RefreshTokenDTO token);
}
