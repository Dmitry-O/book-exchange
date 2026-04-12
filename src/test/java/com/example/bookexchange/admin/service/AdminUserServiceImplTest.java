package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.BanUserDTO;
import com.example.bookexchange.admin.dto.UserAdminDTO;
import com.example.bookexchange.admin.mapper.AdminMapper;
import com.example.bookexchange.auth.repository.RefreshTokenRepository;
import com.example.bookexchange.auth.repository.VerificationTokenRepository;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.SoftDeleteFilterHelper;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.support.TestReportStrings;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_CANT_BAN_YOURSELF;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_REQUEST_NOT_VALID;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_RIGHTS_GIVEN;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_USER_ALREADY_ADMIN;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_USER_BANNED;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_USER_DELETED;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_USER_NOT_ADMIN;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_USER_UNBANNED;
import static com.example.bookexchange.common.result.ResultFactory.ok;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private SoftDeleteFilterHelper softDeleteFilterHelper;

    @Mock
    private VersionedEntityTransitionHelper versionedEntityTransitionHelper;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void shouldAddAdminRole_whenAdminPromotesNonAdminUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        UserAdminDTO dto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(adminMapper.userToUserAdminDto(user)).thenReturn(dto);

        Result<UserAdminDTO> result = adminUserService.giveAdminRights(admin, user.getId());

        assertSuccess(result, HttpStatus.OK, ADMIN_RIGHTS_GIVEN);
        assertThat(user.getRoles()).contains(UserRole.ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    void shouldReturnConflict_whenAdminPromotesExistingAdmin() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        user.addRole(UserRole.ADMIN);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Result<UserAdminDTO> result = adminUserService.giveAdminRights(admin, user.getId());

        assertFailure(result, ADMIN_USER_ALREADY_ADMIN, HttpStatus.CONFLICT);
        verify(userRepository, never()).save(user);
    }

    @Test
    void shouldReturnBadRequest_whenAdminDemotesNonAdminUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Result<UserAdminDTO> result = adminUserService.revokeAdminRights(admin, user.getId());

        assertFailure(result, ADMIN_USER_NOT_ADMIN, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnBadRequest_whenAdminBansThemself() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        BanUserDTO dto = UnitTestDataFactory.permanentBanDto();
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));

        Result<UserAdminDTO> result = adminUserService.banUserById(admin, user.getId(), dto, user.getVersion());

        assertFailure(result, ADMIN_CANT_BAN_YOURSELF, HttpStatus.BAD_REQUEST);
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void shouldReturnBadRequest_whenAdminBanConfigurationIsInvalid() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        BanUserDTO dto = BanUserDTO.builder().banReason(TestReportStrings.banReason("Spam")).build();
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));

        Result<UserAdminDTO> result = adminUserService.banUserById(admin, user.getId(), dto, user.getVersion());

        assertFailure(result, ADMIN_REQUEST_NOT_VALID, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldPersistPermanentBanAndDeleteRefreshTokens_whenAdminBansUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        BanUserDTO dto = UnitTestDataFactory.permanentBanDto();
        UserAdminDTO adminDto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(adminMapper.userToUserAdminDto(user)).thenReturn(adminDto);

        Result<UserAdminDTO> result = adminUserService.banUserById(admin, user.getId(), dto, user.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_USER_BANNED);
        assertThat(user.isBannedPermanently()).isTrue();
        assertThat(user.getBanReason()).isEqualTo(dto.getBanReason());
        verify(refreshTokenRepository).deleteByUserId(user.getId());
    }

    @Test
    void shouldClearBanState_whenAdminUnbansUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        user.setBannedPermanently(true);
        user.setBanReason(TestReportStrings.banReason("Fraud"));
        user.setBannedUntil(UnitTestDataFactory.futureInstant());
        UserAdminDTO adminDto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(userRepository.save(user)).thenReturn(user);
        when(adminMapper.userToUserAdminDto(user)).thenReturn(adminDto);

        Result<UserAdminDTO> result = adminUserService.unbanUserById(admin, user.getId(), user.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_USER_UNBANNED);
        assertThat(user.isBannedPermanently()).isFalse();
        assertThat(user.getBannedUntil()).isNull();
        assertThat(user.getBanReason()).isNull();
    }

    @Test
    void shouldAnonymizeAndSoftDeleteUserData_whenAdminDeletesUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Target book", user);
        UserAdminDTO adminDto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(bookRepository.findAllByUserIdAndDeletedAtIsNull(user.getId())).thenReturn(List.of(book));
        when(adminMapper.userToUserAdminDto(user)).thenReturn(adminDto);
        when(imageStorageService.deleteAllUserImages(user.getId())).thenReturn(ok(null));

        Result<UserAdminDTO> result = adminUserService.deleteUser(admin, user.getId(), user.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_USER_DELETED);
        assertThat(user.getEmail()).isEqualTo("anonymized-" + user.getId() + "@anonymized.anonymized");
        assertThat(user.getPhotoUrl()).isNull();
        assertThat(book.getDeletedAt()).isNotNull();
        assertThat(book.getPhotoUrl()).isNull();
        verify(refreshTokenRepository).deleteByUserId(user.getId());
        verify(verificationTokenRepository).deleteByUserId(user.getId());
        verify(imageStorageService).deleteAllUserImages(user.getId());
    }
}
