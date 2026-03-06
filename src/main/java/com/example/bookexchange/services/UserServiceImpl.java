package com.example.bookexchange.services;

import com.example.bookexchange.controllers.NotFoundException;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.mappers.UserMapper;
import com.example.bookexchange.models.*;
import com.example.bookexchange.repositories.RefreshTokenRepository;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.repositories.VerificationTokenRepository;
import jakarta.persistence.EntityExistsException;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.*;

@Service
@AllArgsConstructor
public class UserServiceImpl extends BaseServiceImpl<User, Long> implements UserService {

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private VerificationTokenRepository verificationTokenRepository;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;

    @Transactional(readOnly = true)
    @Override
    public UserDTO getUser(Long userId) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        return userMapper.userToUserDto(user);
    }

    @Transactional
    @Override
    public VerificationTokenDTO createUser(UserCreateDTO dto) {
        String email = dto.getEmail();
        String nickname = dto.getNickname();

        userRepository.findByEmail(email).ifPresent(user -> {
            throw new EntityExistsException("Es gibt bereits ein Benutzer mit diesem Email. Wählen Sie bitte ein anderes");
        });

        userRepository.findByNickname(nickname).ifPresent(user -> {
            throw new EntityExistsException("Es gibt bereits ein Benutzer mit diesem Nickname. Wählen Sie bitte einen anderen");
        });

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));

        User user = userMapper.userDtoToUser(dto);
        user.addRole(UserRole.USER);
        User savedUser = userRepository.save(user);

        return VerificationTokenDTO
                .builder()
                .message("Ihr Konto wurde erfolgreich registriert. Bitte überprüfen Sie jetzt Ihre E-mail Adresse")
                .token(createVerificationToken(savedUser, TokenType.CONFIRM_EMAIL))
                .build();
    }

    @Transactional
    @Override
    public void updateUser(Long userId, UserUpdateDTO dto) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        if (!user.getNickname().equals(dto.getNickname())) {
            String nickname = dto.getNickname();

            if (userRepository.findByNickname(nickname).isPresent()) {
                throw new EntityExistsException("Es gibt bereits ein Benutzer mit diesem nickname. Wählen Sie bitte einen anderen");
            }
        }

        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getPhotoBase64() != null) user.setPhotoBase64(dto.getPhotoBase64());

        userRepository.save(user);
    }

    @Transactional
    @Override
    public void deleteUser(Long userId) {
        findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        userRepository.deleteById(userId);
    }

    @Transactional
    @Override
    public AuthResponseDTO loginUser(AuthRequestDTO requestDTO) {
        User user = userRepository.findByEmail(requestDTO.getEmail()).orElseThrow();

        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("Falsches Passwort");
        }

        if (!user.isEmailVerified()) {
            verificationTokenRepository.findByUserAndType(user, TokenType.CONFIRM_EMAIL).ifPresent(verificationToken -> verificationTokenRepository.deleteById(verificationToken.getId()));

            throw new RuntimeException("Ihr Konto wurde noch nicht verifiziert");
        }

        Instant bannedUntil = user.getBannedUntil();
        String banReason = user.getBanReason() != null ? (" Grund für die Sperrung: " + user.getBanReason()) : null;

        if (user.isBannedPermanently()) {
            throw new RuntimeException("Der Bunutzer wurde dauerhaft gesperrt." + banReason);
        } else if (bannedUntil != null && bannedUntil.isAfter(Instant.now())) {
            throw new RuntimeException("Der Bunutzer wurde bis " + bannedUntil + " gesperrt." + banReason);
        } else if (bannedUntil != null && bannedUntil.isBefore(Instant.now())) {
            user.setBannedUntil(null);
            user.setBanReason(null);

            userRepository.save(user);
        }

        return AuthResponseDTO
                .builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(createRefreshToken(user))
                .build();
    }

    @Transactional
    @Override
    public String createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plus(30, DAYS));

        return refreshTokenRepository.save(refreshToken).getToken();
    }

    @Transactional
    @Override
    public String createVerificationToken(User user, TokenType tokenType) {
        VerificationToken verificationToken = new VerificationToken();

        Instant expiryDate = Instant.now().plus(tokenType == TokenType.CONFIRM_EMAIL ? 24 : 15, tokenType == TokenType.CONFIRM_EMAIL ? HOURS : MINUTES);

        verificationToken.setToken(UUID.randomUUID().toString());
        verificationToken.setExpiryDate(expiryDate);
        verificationToken.setType(tokenType);
        verificationToken.setUser(user);

        return verificationTokenRepository.save(verificationToken).getToken();
    }

    @Transactional
    @Override
    public String refreshAccessToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow();

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);

            throw new RuntimeException("Das Refresh Token ist abgelaufen");
        }

        return jwtService.generateToken(refreshToken.getUser());
    }

    @Transactional
    @Override
    public void resetPassword(User user, UserResetPasswordDTO dto) {
        if (dto.getCurrentPassword().equals(dto.getNewPassword())) {
            throw new RuntimeException("Die Passwörter dürfen nicht identisch sein");
        } else if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Das aktuelle Passwort ist falsch");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    @Override
    public void confirmRegistration(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token).orElseThrow(() -> new RuntimeException("Das Token wurde nicht gefunden"));

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            verificationTokenRepository.deleteById(verificationToken.getId());

            throw new RuntimeException("Das Token ist bereits abgelaufen");
        } else if (!verificationToken.getType().equals(TokenType.CONFIRM_EMAIL)) {
            throw new RuntimeException("Das Token ist ungültig");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationTokenRepository.deleteById(verificationToken.getId());
    }

    @Transactional
    @Override
    public VerificationTokenDTO forgotPassword(UserForgotPasswordDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new NotFoundException("Der Benutzer mit Email " + dto.getEmail() + " wurde nicht gefunden"));

        return VerificationTokenDTO
                .builder()
                .token(createVerificationToken(user, TokenType.RESET_PASSWORD))
                .message("Wir haben Ihnen eine Anleitung zum Zurücksetzen des Passworts auf Ihre E-mail Adresse geschikt")
                .build();
    }

    @Transactional
    @Override
    public void resetForgottenPassword(UserResetForgottenPasswordDTO dto) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(dto.getToken()).orElseThrow(() -> new RuntimeException("Das Token wurde nicht gefunden"));

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            verificationTokenRepository.deleteById(verificationToken.getId());

            throw new RuntimeException("Das Token ist bereits abgelaufen");
        } else if (!verificationToken.getType().equals(TokenType.RESET_PASSWORD)) {
            throw new RuntimeException("Das Token ist ungültig");
        }

        User user = verificationToken.getUser();
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        verificationTokenRepository.deleteById(verificationToken.getId());
    }

    @Transactional
    @Override
    public VerificationTokenDTO resendEmailConfirmation(UserResendEmailConfirmationDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new NotFoundException("Der Benutzer mit Email " + dto.getEmail() + " wurde nicht gefunden"));

        verificationTokenRepository.findByUserAndType(user, TokenType.CONFIRM_EMAIL).ifPresent(verificationToken -> verificationTokenRepository.deleteById(verificationToken.getId()));

        return VerificationTokenDTO
                .builder()
                .message("Bitte überprüfen Sie jetzt Ihre E-mail Adresse")
                .token(createVerificationToken(user, TokenType.CONFIRM_EMAIL))
                .build();
    }
}
