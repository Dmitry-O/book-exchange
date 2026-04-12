package com.example.bookexchange.user.service;

import com.example.bookexchange.auth.dto.RefreshTokenDTO;
import com.example.bookexchange.auth.service.RefreshTokenService;
import com.example.bookexchange.auth.service.VerificationTokenService;
import com.example.bookexchange.book.service.BookService;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.dto.UserDTO;
import com.example.bookexchange.user.dto.UserResetPasswordDTO;
import com.example.bookexchange.user.dto.UserUpdateDTO;
import com.example.bookexchange.user.mapper.UserMapper;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.example.bookexchange.common.i18n.MessageKey.AUTH_LOGOUT;
import static com.example.bookexchange.common.i18n.MessageKey.AUTH_NICKNAME_ALREADY_EXISTS;
import static com.example.bookexchange.common.i18n.MessageKey.AUTH_PASSWORD_CHANGED;
import static com.example.bookexchange.common.i18n.MessageKey.AUTH_SAME_PASSWORDS;
import static com.example.bookexchange.common.i18n.MessageKey.AUTH_WRONG_ACTUAL_PASSWORD;
import static com.example.bookexchange.common.i18n.MessageKey.USER_ACCOUNT_DELETED;
import static com.example.bookexchange.common.i18n.MessageKey.USER_PROFILE_PHOTO_DELETED;
import static com.example.bookexchange.common.result.ResultFactory.ok;
import static com.example.bookexchange.common.result.ResultFactory.okMessage;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private BookService bookService;

    @Mock
    private VersionedEntityTransitionHelper versionedEntityTransitionHelper;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldReturnConflict_whenUpdateUserNicknameAlreadyExists() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        UserUpdateDTO dto = UnitTestDataFactory.userUpdateDto("taken_nickname");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(userRepository.findByNickname(dto.getNickname())).thenReturn(Optional.of(new User()));

        Result<UserDTO> result = userService.updateUser(user.getId(), dto, user.getVersion());

        assertFailure(result, AUTH_NICKNAME_ALREADY_EXISTS, HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldAnonymizeUserAndDeleteTokens_whenDeleteUserVersionMatches() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(imageStorageService.deleteAllUserImages(user.getId())).thenReturn(ok(null));
        when(refreshTokenService.deleteUserTokens(user)).thenReturn(ok(user));
        when(verificationTokenService.deleteUserTokens(user)).thenReturn(ok(user));

        Result<Void> result = userService.deleteUser(user.getId(), user.getVersion());

        assertSuccess(result, HttpStatus.OK, USER_ACCOUNT_DELETED);
        assertThat(user.getEmail()).isEqualTo("anonymized-" + user.getId() + "@anonymized.anonymized");
        assertThat(user.getNickname()).isEqualTo("anonymized-" + user.getId());
        assertThat(user.getPassword()).isEmpty();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getPhotoUrl()).isNull();
        verify(bookService).softDeleteBooks(any(User.class), any());
        verify(imageStorageService).deleteAllUserImages(user.getId());
    }

    @Test
    void shouldDeleteUserPhoto_whenVersionMatchesAndPhotoExists() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        UserDTO userDto = mock(UserDTO.class);
        user.setPhotoUrl("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/profile_photo_test.jpg");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(imageStorageService.deleteUserProfileImage(user.getId())).thenReturn(ok(null));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.userToUserDto(user)).thenReturn(userDto);

        Result<UserDTO> result = userService.deleteUserPhoto(user.getId(), user.getVersion());

        assertSuccess(result, HttpStatus.OK, USER_PROFILE_PHOTO_DELETED);
        assertThat(user.getPhotoUrl()).isNull();
        verify(imageStorageService).deleteUserProfileImage(user.getId());
        verify(userRepository).save(user);
    }

    @Test
    void shouldKeepExistingPhoto_whenUpdateUserReceivesBlankPhotoBase64() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        user.setPhotoUrl("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/profile_photo_existing.jpg");

        UserUpdateDTO dto = UnitTestDataFactory.userUpdateDto("reader_updated");
        dto.setPhotoBase64(" ");

        UserDTO userDto = mock(UserDTO.class);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(userRepository.findByNickname(dto.getNickname())).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.userToUserDto(user)).thenReturn(userDto);

        Result<UserDTO> result = userService.updateUser(user.getId(), dto, user.getVersion());

        assertSuccess(result, HttpStatus.OK, MessageKey.USER_PROFILE_UPDATED);
        assertThat(user.getPhotoUrl()).isEqualTo("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/profile_photo_existing.jpg");
        verify(imageStorageService, never()).replaceUserProfileImage(any(), any());
    }

    @Test
    void shouldReturnBadRequest_whenResetPasswordPasswordsAreSame() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        UserResetPasswordDTO dto = UnitTestDataFactory.resetPasswordDto("Password-123", "Password-123");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));

        Result<Void> result = userService.resetPassword(user.getId(), dto, user.getVersion());

        assertFailure(result, AUTH_SAME_PASSWORDS, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnBadRequest_whenResetPasswordCurrentPasswordIsWrong() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        UserResetPasswordDTO dto = UnitTestDataFactory.resetPasswordDto("wrong-password", "NewPassword-123");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())).thenReturn(false);

        Result<Void> result = userService.resetPassword(user.getId(), dto, user.getVersion());

        assertFailure(result, AUTH_WRONG_ACTUAL_PASSWORD, HttpStatus.BAD_REQUEST);
        verify(auditService).log(any());
    }

    @Test
    void shouldSaveEncodedPassword_whenResetPasswordPayloadIsValid() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        UserResetPasswordDTO dto = UnitTestDataFactory.resetPasswordDto("Password-123", "NewPassword-123");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(dto.getNewPassword())).thenReturn("new-encoded-password");
        when(userRepository.save(user)).thenReturn(user);

        Result<Void> result = userService.resetPassword(user.getId(), dto, user.getVersion());

        assertSuccess(result, HttpStatus.OK, AUTH_PASSWORD_CHANGED);
        assertThat(user.getPassword()).isEqualTo("new-encoded-password");
        verify(userRepository).save(user);
    }

    @Test
    void shouldDelegateToRefreshTokenService_whenLogoutIsCalled() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setToken("refresh-token");

        when(refreshTokenService.deleteToken(user.getId(), dto.getToken())).thenReturn(okMessage(MessageKey.AUTH_LOGOUT));

        Result<Void> result = userService.logout(user.getId(), dto);

        assertSuccess(result, HttpStatus.OK, AUTH_LOGOUT);
        verify(refreshTokenService).deleteToken(user.getId(), dto.getToken());
    }
}
