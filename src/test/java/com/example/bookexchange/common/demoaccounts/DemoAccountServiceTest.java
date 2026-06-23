package com.example.bookexchange.common.demoaccounts;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.result.Success;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(MockitoExtension.class)
class DemoAccountServiceTest {

    private static final String DEMO_PASSWORD = "DemoPassword123!";
    private static final String DEMO_EMAIL_PATTERN = "%.demo@example.com";

    @Mock
    private UserRepository userRepository;

    private AppProperties appProperties;
    private DemoAccountService demoAccountService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setRuntimeEnv("demo");
        appProperties.getDemoAccounts().setEmailPattern(DEMO_EMAIL_PATTERN);
        appProperties.getDemoAccounts().setPassword(DEMO_PASSWORD);

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        demoAccountService = new DemoAccountService(appProperties, userRepository, passwordEncoder);
    }

    @Test
    void shouldReturnOnlyNonAdminDemoAccountsWithConfiguredPassword() {
        User reader = user("reader.demo@example.com", "demo_reader", Set.of(UserRole.USER), DEMO_PASSWORD);
        User owner = user("owner.demo@example.com", "demo_owner", Set.of(UserRole.USER), DEMO_PASSWORD);
        User admin = user("admin.demo@example.com", "demo_admin", Set.of(UserRole.USER, UserRole.ADMIN), DEMO_PASSWORD);
        User wrongPassword = user("wrong.demo@example.com", "demo_wrong", Set.of(UserRole.USER), "OtherPassword123!");

        when(userRepository.findVerifiedActiveDemoAccountCandidates(DEMO_EMAIL_PATTERN))
                .thenReturn(List.of(reader, admin, wrongPassword, owner));

        Success<List<DemoAccountDTO>> success = assertSuccess(demoAccountService.findDemoAccounts(), OK);

        assertThat(success.body())
                .extracting(DemoAccountDTO::email)
                .containsExactly("owner.demo@example.com", "reader.demo@example.com");
        assertThat(success.body())
                .allSatisfy(account -> assertThat(account.password()).isEqualTo(DEMO_PASSWORD));
    }

    @Test
    void shouldReturnEmptyListOutsideDemoRuntime() {
        appProperties.setRuntimeEnv("local");

        Success<List<DemoAccountDTO>> success = assertSuccess(demoAccountService.findDemoAccounts(), OK);

        assertThat(success.body()).isEmpty();
        verify(userRepository, never()).findVerifiedActiveDemoAccountCandidates(DEMO_EMAIL_PATTERN);
    }

    @Test
    void shouldReturnEmptyList_whenDemoPasswordIsNotConfigured() {
        appProperties.getDemoAccounts().setPassword("");

        Success<List<DemoAccountDTO>> success = assertSuccess(demoAccountService.findDemoAccounts(), OK);

        assertThat(success.body()).isEmpty();
        verify(userRepository, never()).findVerifiedActiveDemoAccountCandidates(DEMO_EMAIL_PATTERN);
    }

    private User user(String email, String nickname, Set<UserRole> roles, String rawPassword) {
        User user = new User();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setRoles(roles);
        user.setPassword(new BCryptPasswordEncoder().encode(rawPassword));
        user.setEmailVerified(true);
        return user;
    }
}
