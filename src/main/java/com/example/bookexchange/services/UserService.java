package com.example.bookexchange.services;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.TokenType;
import com.example.bookexchange.models.User;

public interface UserService {

    UserDTO getUser(Long userId);

    String createUser(UserCreateDTO dto);

    String updateUser(Long userId, UserUpdateDTO dto);

    String deleteUser(Long userId);

    AuthResponseDTO loginUser(AuthRequestDTO requestDTO);

    String createRefreshToken(User user);

    String refreshAccessToken(String token);

    String resetPassword(Long userId, UserResetPasswordDTO dto);

    String createVerificationToken(User user, TokenType tokenType);

    String confirmRegistration(String token);

    String forgotPassword(UserForgotPasswordDTO dto);

    String resetForgottenPassword(String token, UserResetForgottenPasswordDTO dto);

    String resendEmailConfirmation(UserResendEmailConfirmationDTO dto);
}
