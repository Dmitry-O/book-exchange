package com.example.bookexchange.services;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.exception.EntityExistsException;
import com.example.bookexchange.exception.ForbiddenException;
import com.example.bookexchange.exception.NotFoundException;
import com.example.bookexchange.mappers.UserMapper;
import com.example.bookexchange.models.*;
import com.example.bookexchange.repositories.RefreshTokenRepository;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.repositories.VerificationTokenRepository;
import com.example.bookexchange.util.Helper;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.temporal.ChronoUnit.*;

@Service
@AllArgsConstructor
public class UserServiceImpl extends BaseServiceImpl<User, Long> implements UserService {

    private final MessageService messageService;
    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private VerificationTokenRepository verificationTokenRepository;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private EmailService emailService;
    private final Helper helper;

    @Transactional(readOnly = true)
    @Override
    public User getUser(Long userId) {
        return findOrThrow(userRepository, userId, MessageKey.USER_ACCOUNT_NOT_FOUND);
    }

    @Transactional
    @Override
    public String createUser(UserCreateDTO dto) {
        String email = dto.getEmail();
        String nickname = dto.getNickname();

        AtomicReference<String> messageToReturn = new AtomicReference<>();

        userRepository.findByEmail(email).ifPresent(user -> {
            if (passwordEncoder.matches(dto.getPassword(), user.getPassword()) && !user.isEmailVerified()) {
                messageToReturn.set(messageService.getMessage(MessageKey.EMAIL_VERIFY_ACCOUNT));
            } else {
                throw new EntityExistsException(MessageKey.AUTH_EMAIL_ALREADY_EXISTS);
            }
        });

        if (messageToReturn.get() != null) {
            return messageToReturn.get();
        }

        userRepository.findByNickname(nickname).ifPresent(user -> {
            throw new EntityExistsException(MessageKey.AUTH_NICKNAME_ALREADY_EXISTS);
        });

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));

        User user = userMapper.userDtoToUser(dto);
        user.addRole(UserRole.USER);
        User savedUser = userRepository.save(user);

        emailService.buildAndSendEmail(user.getEmail(), createVerificationToken(savedUser, TokenType.CONFIRM_EMAIL), EmailType.CONFIRM_EMAIL);

        return messageService.getMessage(MessageKey.AUTH_ACCOUNT_REGISTERED);
    }

    @Transactional
    @Override
    public String updateUser(Long userId, UserUpdateDTO dto, Long version) {
        User user = findOrThrow(userRepository, userId, MessageKey.USER_ACCOUNT_NOT_FOUND);

        helper.checkEntityVersion(user.getVersion(), version);

        if (!user.getNickname().equals(dto.getNickname())) {
            String nickname = dto.getNickname();

            if (userRepository.findByNickname(nickname).isPresent()) {
                throw new EntityExistsException(MessageKey.AUTH_NICKNAME_ALREADY_EXISTS);
            }
        }

        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getPhotoBase64() != null) user.setPhotoBase64(dto.getPhotoBase64());
        if (dto.getLocale() != null) user.setLocale(dto.getLocale());

        userRepository.save(user);

        return messageService.getMessage(MessageKey.USER_PROFILE_UPDATED);
    }

    @Transactional
    @Override
    public String deleteUser(Long userId, Boolean isAdminDeleting, Long version) {
        User user = findOrThrow(userRepository, userId, MessageKey.USER_ACCOUNT_NOT_FOUND);

        helper.checkEntityVersion(user.getVersion(), version);

        user.setEmail("anonymized@anonymized.anonymized");
        user.setNickname("anonymized");
        user.setPhotoBase64(null);
        user.setPassword("");
        user.setDeletedAt(Instant.now());

        for (Book book : new HashSet<>(user.getBooks())) {
            if (book.getDeletedAt() == null) {
                book.setDeletedAt(Instant.now());
            }
        }

        refreshTokenRepository.deleteAll(new HashSet<>(user.getRefreshTokens()));
        verificationTokenRepository.deleteAll(new HashSet<>(user.getVerificationToken()));

        return isAdminDeleting ? messageService.getMessage(MessageKey.ADMIN_USER_DELETED, user.getEmail()) : messageService.getMessage(MessageKey.USER_ACCOUNT_DELETED);
    }

    @Transactional
    @Override
    public AuthResponseDTO loginUser(AuthRequestDTO requestDTO) {
        User user = userRepository.findByEmail(requestDTO.getEmail()).orElseThrow(() -> new NotFoundException(MessageKey.AUTH_WRONG_EMAIL));

        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            throw new BadRequestException(MessageKey.AUTH_WRONG_PASSWORD);
        }

        if (!user.isEmailVerified()) {
            verificationTokenRepository.findByUserAndType(user, TokenType.CONFIRM_EMAIL).ifPresent(verificationToken -> verificationTokenRepository.deleteById(verificationToken.getId()));

            throw new ForbiddenException(MessageKey.AUTH_ACCOUNT_NOT_VERIFIED);
        }

        Instant bannedUntil = user.getBannedUntil();
        String banReason = user.getBanReason() != null ? user.getBanReason() : null;

        if (user.isBannedPermanently()) {
            throw new ForbiddenException(MessageKey.AUTH_PERMANENTLY_BANNED, banReason);
        } else if (bannedUntil != null && bannedUntil.isAfter(Instant.now())) {
            throw new ForbiddenException(MessageKey.AUTH_TEMPORARILY_BANNED, bannedUntil, banReason);
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
        long tokenLiveTime;

        ChronoUnit timeUnit = switch (tokenType) {
            case TokenType.CONFIRM_EMAIL -> {
                tokenLiveTime = 24L;
                yield HOURS;
            }
            case TokenType.RESET_PASSWORD, TokenType.DELETE_ACCOUNT -> {
                tokenLiveTime = 15L;
                yield MINUTES;
            }
            default -> throw new BadRequestException(MessageKey.AUTH_WRONG_TOKEN);
        };

        Instant expiryDate = Instant.now().plus(tokenLiveTime, timeUnit);

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

            throw new BadRequestException(MessageKey.AUTH_REFRESH_TOKEN_EXPIRED);
        }

        return jwtService.generateToken(refreshToken.getUser());
    }

    @Transactional
    @Override
    public String resetPassword(Long userId, UserResetPasswordDTO dto, Long version) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException(MessageKey.USER_ACCOUNT_NOT_FOUND));

        helper.checkEntityVersion(user.getVersion(), version);

        if (dto.getCurrentPassword().equals(dto.getNewPassword())) {
            throw new BadRequestException(MessageKey.AUTH_SAME_PASSWORDS);
        } else if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException(MessageKey.AUTH_WRONG_ACTUAL_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        return messageService.getMessage(MessageKey.AUTH_PASSWORD_CHANGED);
    }

    @Transactional
    @Override
    public String confirmRegistration(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token).orElseThrow(() -> new NotFoundException(MessageKey.AUTH_TOKEN_NOT_FOUND));

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            verificationTokenRepository.deleteById(verificationToken.getId());

            throw new BadRequestException(MessageKey.AUTH_TOKEN_EXPIRED);
        } else if (!verificationToken.getType().equals(TokenType.CONFIRM_EMAIL)) {
            throw new BadRequestException(MessageKey.AUTH_TOKEN_NOT_VALID);
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationTokenRepository.deleteById(verificationToken.getId());

        return messageService.getMessage(MessageKey.AUTH_REGISTRATION_COMPLETED);
    }

    @Transactional
    @Override
    public String forgotPassword(UserForgotPasswordDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new NotFoundException(MessageKey.AUTH_EMAIL_NOT_FOUND));

        if (!user.isEmailVerified()) {
            throw new ForbiddenException(MessageKey.AUTH_ACCOUNT_NOT_VERIFIED);
        }

        emailService.buildAndSendEmail(user.getEmail(), createVerificationToken(user, TokenType.RESET_PASSWORD), EmailType.RESET_PASSWORD);

        return messageService.getMessage(MessageKey.EMAIL_RESET_PASSWORD);
    }

    @Transactional
    @Override
    public String resetForgottenPassword(String token, UserResetForgottenPasswordDTO dto) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token).orElseThrow(() -> new NotFoundException(MessageKey.AUTH_TOKEN_NOT_FOUND));

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            verificationTokenRepository.deleteById(verificationToken.getId());

            throw new BadRequestException(MessageKey.AUTH_TOKEN_EXPIRED);
        } else if (!verificationToken.getType().equals(TokenType.RESET_PASSWORD)) {
            throw new BadRequestException(MessageKey.AUTH_TOKEN_NOT_VALID);
        }

        User user = verificationToken.getUser();
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        verificationTokenRepository.deleteById(verificationToken.getId());

        return messageService.getMessage(MessageKey.AUTH_PASSWORD_CHANGED);
    }

    @Transactional
    @Override
    public String resendEmailConfirmation(UserResendEmailConfirmationDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new NotFoundException(MessageKey.AUTH_EMAIL_NOT_FOUND));

        if (user.isEmailVerified()) {
            throw new BadRequestException(MessageKey.AUTH_ACCOUNT_ALREADY_VERIFIED);
        }

        verificationTokenRepository.findByUserAndType(user, TokenType.CONFIRM_EMAIL).ifPresent(verificationToken -> verificationTokenRepository.deleteById(verificationToken.getId()));

        emailService.buildAndSendEmail(user.getEmail(), createVerificationToken(user, TokenType.CONFIRM_EMAIL), EmailType.CONFIRM_EMAIL);

        return messageService.getMessage(MessageKey.EMAIL_VERIFY_ACCOUNT);
    }

    @Transactional
    @Override
    public String initiateDeleteAccount(UserInitiateDeleteAccountDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new NotFoundException(MessageKey.AUTH_EMAIL_NOT_FOUND));

        emailService.buildAndSendEmail(user.getEmail(), createVerificationToken(user, TokenType.DELETE_ACCOUNT), EmailType.DELETE_ACCOUNT);

        return messageService.getMessage(MessageKey.EMAIL_DELETE_ACCOUNT);
    }

    @Transactional
    @Override
    public String deleteAccount(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token).orElseThrow(() -> new NotFoundException(MessageKey.AUTH_TOKEN_NOT_FOUND));

        return deleteUser(verificationToken.getUser().getId(), false, null);
    }

    @Transactional
    @Override
    public String logout(Long userId, RefreshTokenDTO dto) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndUserId(dto.getToken(), userId).orElseThrow(() -> new NotFoundException(MessageKey.AUTH_TOKEN_NOT_FOUND));

        refreshTokenRepository.delete(refreshToken);

        return messageService.getMessage(MessageKey.AUTH_LOGOUT);
    }
}
