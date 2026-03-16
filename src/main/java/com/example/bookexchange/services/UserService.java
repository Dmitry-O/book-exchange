package com.example.bookexchange.services;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.TokenType;
import com.example.bookexchange.models.User;

public interface UserService {

    User getUser(Long userId);

    String createUser(UserCreateDTO dto);

    String updateUser(Long userId, UserUpdateDTO dto, Long version);

    String deleteUser(Long userId, Boolean isAdminDeleting, Long version);

    AuthResponseDTO loginUser(AuthRequestDTO requestDTO);

    String createRefreshToken(User user);

    String refreshAccessToken(String token);

    String resetPassword(Long userId, UserResetPasswordDTO dto, Long version);

    String createVerificationToken(User user, TokenType tokenType);

    String confirmRegistration(String token);

    String forgotPassword(UserForgotPasswordDTO dto);

    String resetForgottenPassword(String token, UserResetForgottenPasswordDTO dto);

    String resendEmailConfirmation(UserResendEmailConfirmationDTO dto);

    String initiateDeleteAccount(UserInitiateDeleteAccountDTO dto);

    String deleteAccount(String token);

    String logout(Long userId, RefreshTokenDTO token);
}
