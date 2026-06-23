package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.BanUserDTO;
import com.example.bookexchange.admin.dto.UserAdminDTO;
import com.example.bookexchange.admin.mapper.AdminMapper;
import com.example.bookexchange.auth.repository.RefreshTokenRepository;
import com.example.bookexchange.auth.repository.VerificationTokenRepository;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.book.search.BookSearchIndexService;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.SoftDeleteFilterHelper;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.notification.NotificationDispatchService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.storage.ImageStorageService;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_CANT_BAN_YOURSELF;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_REQUEST_NOT_VALID;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_RIGHTS_GIVEN;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_RIGHTS_REVOKED;
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

    @Mock
    private BookSearchIndexService bookSearchIndexService;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @Mock
    private ExchangeRepository exchangeRepository;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void shouldKeepDeletedUsersAtEnd_whenAdminListsAllUsersWithoutExplicitSort() {
        User adminUser = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        adminUser.addRole(UserRole.ADMIN);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(softDeleteFilterHelper.runWithoutDeletedFilter(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<Object> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Result<org.springframework.data.domain.Page<UserAdminDTO>> result = adminUserService.findUsers(
                adminUser.getId(),
                UnitTestDataFactory.pageQuery(0, 20),
                null,
                Set.of(),
                false,
                com.example.bookexchange.user.model.UserType.ALL
        );

        assertSuccess(result, HttpStatus.OK);
        verify(userRepository).findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().toList()).extracting(order -> order.getProperty())
                .containsExactly("deletedAt", "createdAt", "id");
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("deletedAt").getDirection().name()).isEqualTo("ASC");
    }

    @Test
    void shouldAddAdminRole_whenAdminPromotesNonAdminUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        UserAdminDTO dto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userRepository.findByEmail(admin.getUsername())).thenReturn(Optional.empty());
        when(adminMapper.userToUserAdminDto(user)).thenReturn(dto);

        Result<UserAdminDTO> result = adminUserService.giveAdminRights(admin, user.getId());

        assertSuccess(result, HttpStatus.OK, ADMIN_RIGHTS_GIVEN);
        assertThat(user.getRoles()).contains(UserRole.ADMIN);
        verify(userRepository).save(user);
        verify(notificationDispatchService).sendAdminRightsGrantedNotification(user, admin.getUsername());
    }

    @Test
    void shouldNotifySuperAdminActor_whenSuperAdminPromotesUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        User superAdmin = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "super@example.com", "super_admin");
        superAdmin.addRole(UserRole.SUPER_ADMIN);
        UserAdminDTO dto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal(superAdmin.getEmail());

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userRepository.findByEmail(superAdmin.getEmail())).thenReturn(Optional.of(superAdmin));
        when(adminMapper.userToUserAdminDto(user)).thenReturn(dto);

        Result<UserAdminDTO> result = adminUserService.giveAdminRights(admin, user.getId());

        assertSuccess(result, HttpStatus.OK, ADMIN_RIGHTS_GIVEN);
        verify(notificationDispatchService).sendAdminRightsGrantedNotification(user, admin.getUsername());
        verify(notificationDispatchService).sendAdminRightsActorNotification(superAdmin, user, true);
    }

    @Test
    void shouldNotifySuperAdminActor_whenSuperAdminRevokesAdminRights() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        user.addRole(UserRole.ADMIN);
        User superAdmin = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "super@example.com", "super_admin");
        superAdmin.addRole(UserRole.SUPER_ADMIN);
        UserAdminDTO dto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal(superAdmin.getEmail());

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userRepository.findByEmail(superAdmin.getEmail())).thenReturn(Optional.of(superAdmin));
        when(adminMapper.userToUserAdminDto(user)).thenReturn(dto);

        Result<UserAdminDTO> result = adminUserService.revokeAdminRights(admin, user.getId());

        assertSuccess(result, HttpStatus.OK, ADMIN_RIGHTS_REVOKED);
        verify(notificationDispatchService).sendAdminRightsRevokedNotification(user, admin.getUsername());
        verify(notificationDispatchService).sendAdminRightsActorNotification(superAdmin, user, false);
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
    void shouldRemoveAdminRole_whenAdminRevokesAdminRights() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        user.addRole(UserRole.ADMIN);
        UserAdminDTO dto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userRepository.findByEmail(admin.getUsername())).thenReturn(Optional.empty());
        when(adminMapper.userToUserAdminDto(user)).thenReturn(dto);

        Result<UserAdminDTO> result = adminUserService.revokeAdminRights(admin, user.getId());

        assertSuccess(result, HttpStatus.OK, ADMIN_RIGHTS_REVOKED);
        assertThat(user.getRoles()).doesNotContain(UserRole.ADMIN);
        verify(notificationDispatchService).sendAdminRightsRevokedNotification(user, admin.getUsername());
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
    void shouldReturnBadRequest_whenAdminBanUntilIsInPast() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        BanUserDTO dto = BanUserDTO.builder()
                .bannedUntil(OffsetDateTime.now().minusMinutes(1))
                .banReason(TestReportStrings.banReason("Spam"))
                .build();
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));

        Result<UserAdminDTO> result = adminUserService.banUserById(admin, user.getId(), dto, user.getVersion());

        assertFailure(result, ADMIN_REQUEST_NOT_VALID, HttpStatus.BAD_REQUEST);
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void shouldPersistPermanentBanAndDeleteRefreshTokens_whenAdminBansUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        user.setBannedUntil(UnitTestDataFactory.futureInstant());
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
        assertThat(user.getBannedUntil()).isNull();
        assertThat(user.getBanReason()).isEqualTo(dto.getBanReason());
        verify(refreshTokenRepository).deleteByUserId(user.getId());
        verify(notificationDispatchService).sendAdminUserBannedNotification(user, admin.getUsername());
    }

    @Test
    void shouldNotifyActor_whenAdminBansUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        User actor = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        BanUserDTO dto = UnitTestDataFactory.permanentBanDto();
        UserAdminDTO adminDto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(userRepository.findByEmail(admin.getUsername())).thenReturn(Optional.of(actor));
        when(adminMapper.userToUserAdminDto(user)).thenReturn(adminDto);

        Result<UserAdminDTO> result = adminUserService.banUserById(admin, user.getId(), dto, user.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_USER_BANNED);
        verify(notificationDispatchService).sendAdminUserBannedActorNotification(actor, user);
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
    void shouldNotifyActorAndTargetUpdates_whenAdminUnbansUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        user.setBannedPermanently(true);
        User actor = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        UserAdminDTO adminDto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(userRepository.findByEmail(admin.getUsername())).thenReturn(Optional.of(actor));
        when(userRepository.save(user)).thenReturn(user);
        when(adminMapper.userToUserAdminDto(user)).thenReturn(adminDto);

        Result<UserAdminDTO> result = adminUserService.unbanUserById(admin, user.getId(), user.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_USER_UNBANNED);
        verify(notificationDispatchService).sendAdminUserUnbannedUpdates(actor, user);
    }

    @Test
    void shouldAnonymizeAndSoftDeleteUserData_whenAdminDeletesUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        user.setPhotoUrl("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/" + user.getId() + "/profile_photo_test.jpg");
        Book book = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Target book", user);
        User actor = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        UserAdminDTO adminDto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(admin.getUsername())).thenReturn(Optional.of(actor));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(bookRepository.findAllByUserIdAndDeletedAtIsNull(user.getId())).thenReturn(List.of(book));
        when(exchangeRepository.findByStatusAndParticipantUserId(ExchangeStatus.PENDING, user.getId())).thenReturn(List.of());
        when(imageStorageService.deleteUserProfileImage(user.getId())).thenReturn(ok(null));
        when(adminMapper.userToUserAdminDto(user)).thenReturn(adminDto);

        Result<UserAdminDTO> result = adminUserService.deleteUser(admin, user.getId(), user.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_USER_DELETED);
        assertThat(user.getEmail()).isEqualTo("anonymized-" + user.getId() + "@anonymized.anonymized");
        assertThat(user.getPhotoUrl()).isNull();
        assertThat(book.getDeletedAt()).isNotNull();
        assertThat(book.getPhotoUrl()).isNotNull();
        verify(imageStorageService).deleteUserProfileImage(user.getId());
        verify(refreshTokenRepository).deleteByUserId(user.getId());
        verify(verificationTokenRepository).deleteByUserId(user.getId());
        verify(notificationDispatchService).sendAdminUserDeletedNotification(
                user.getId(),
                "target@example.com",
                "target_one",
                "en",
                admin.getUsername(),
                user.getDeletedAt()
        );
        verify(notificationDispatchService).sendAdminUserDeletedActorNotification(
                actor,
                user.getId(),
                "target_one",
                "target@example.com",
                null
        );
    }

    @Test
    void shouldDeclinePendingExchanges_whenAdminDeletesUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        User actor = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        User otherUser = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "other@example.com", "other_one");
        Book senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", user);
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", otherUser);
        Exchange pendingExchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                user,
                otherUser,
                senderBook,
                receiverBook,
                ExchangeStatus.PENDING
        );
        pendingExchange.setIsReadBySender(true);
        pendingExchange.setIsReadByReceiver(true);
        UserAdminDTO adminDto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(admin.getUsername())).thenReturn(Optional.of(actor));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(bookRepository.findAllByUserIdAndDeletedAtIsNull(user.getId())).thenReturn(List.of(senderBook));
        when(exchangeRepository.findByStatusAndParticipantUserId(ExchangeStatus.PENDING, user.getId()))
                .thenReturn(List.of(pendingExchange));
        when(adminMapper.userToUserAdminDto(user)).thenReturn(adminDto);

        Result<UserAdminDTO> result = adminUserService.deleteUser(admin, user.getId(), user.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_USER_DELETED);
        assertThat(pendingExchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(pendingExchange.getDeclinerUser()).isNull();
        assertThat(pendingExchange.getAutoDeclined()).isTrue();
        assertThat(pendingExchange.getIsReadBySender()).isFalse();
        assertThat(pendingExchange.getIsReadByReceiver()).isFalse();
        verify(exchangeRepository).save(pendingExchange);
        verify(notificationDispatchService).sendExchangeAutoDeclinedNotifications(List.of(pendingExchange));
    }

    @Test
    void shouldDeclinePendingGiftRequests_whenAdminDeletesUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        User actor = UnitTestDataFactory.user(UnitFixtureIds.ADMIN_USER_ID, "admin@example.com", "admin_one");
        User otherUser = UnitTestDataFactory.user(UnitFixtureIds.BOOK_OWNER_ID, "other@example.com", "other_one");
        Book giftBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Gift book", otherUser);
        giftBook.setIsGift(true);
        Exchange pendingGiftExchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                user,
                otherUser,
                null,
                giftBook,
                ExchangeStatus.PENDING
        );
        pendingGiftExchange.setIsReadBySender(true);
        pendingGiftExchange.setIsReadByReceiver(true);
        UserAdminDTO adminDto = mock(UserAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(admin.getUsername())).thenReturn(Optional.of(actor));
        when(versionedEntityTransitionHelper.requireVersion(any(User.class), any(Long.class), any(String.class), any()))
                .thenReturn(ok(user));
        when(bookRepository.findAllByUserIdAndDeletedAtIsNull(user.getId())).thenReturn(List.of());
        when(exchangeRepository.findByStatusAndParticipantUserId(ExchangeStatus.PENDING, user.getId()))
                .thenReturn(List.of(pendingGiftExchange));
        when(adminMapper.userToUserAdminDto(user)).thenReturn(adminDto);

        Result<UserAdminDTO> result = adminUserService.deleteUser(admin, user.getId(), user.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_USER_DELETED);
        assertThat(pendingGiftExchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(pendingGiftExchange.getDeclinerUser()).isNull();
        assertThat(pendingGiftExchange.getAutoDeclined()).isTrue();
        assertThat(pendingGiftExchange.getIsReadBySender()).isFalse();
        assertThat(pendingGiftExchange.getIsReadByReceiver()).isFalse();
        verify(exchangeRepository).save(pendingGiftExchange);
        verify(notificationDispatchService).sendExchangeAutoDeclinedNotifications(List.of(pendingGiftExchange));
    }
}
