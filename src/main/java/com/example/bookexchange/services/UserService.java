package com.example.bookexchange.services;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.TokenType;
import com.example.bookexchange.models.User;

public interface UserService {

    UserDTO getUser(Long userId);

    String createUser(UserCreateDTO dto);

    void updateUser(Long userId, UserUpdateDTO dto);

    void deleteUser(Long userId);

    AuthResponseDTO loginUser(AuthRequestDTO requestDTO);

    String createRefreshToken(User user);

    String refreshAccessToken(String token);

    void resetPassword(User user, UserResetPasswordDTO dto);

    String createVerificationToken(User user, TokenType tokenType);

    void confirmRegistration(String token);

    String forgotPassword(UserForgotPasswordDTO dto);

    void resetForgottenPassword(String token, UserResetForgottenPasswordDTO dto);

    String resendEmailConfirmation(UserResendEmailConfirmationDTO dto);
}
