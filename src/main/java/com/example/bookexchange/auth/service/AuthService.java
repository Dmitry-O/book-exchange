package com.example.bookexchange.auth.service;

import com.example.bookexchange.auth.dto.AuthLoginRequestDTO;
import com.example.bookexchange.auth.dto.AuthLoginResponseDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.user.dto.*;

public interface AuthService {

    Result<Void> createUser(UserCreateDTO dto);

    Result<AuthLoginResponseDTO> loginUser(AuthLoginRequestDTO requestDTO);

    Result<String> refreshAccessToken(String token);

    Result<Void> confirmRegistration(String token);

    Result<Void> forgotPassword(UserForgotPasswordDTO dto);

    Result<Void> resetForgottenPassword(String token, UserResetForgottenPasswordDTO dto);

    Result<Void> resendEmailConfirmation(UserResendEmailConfirmationDTO dto);

    Result<Void> initiateDeleteAccount(UserInitiateDeleteAccountDTO dto);

    Result<Void> deleteAccount(String token);
}
