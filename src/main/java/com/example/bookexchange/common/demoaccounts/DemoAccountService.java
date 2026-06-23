package com.example.bookexchange.common.demoaccounts;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DemoAccountService {

    private static final String DEMO_RUNTIME_ENV = "demo";
    private static final Set<UserRole> PRIVILEGED_ROLES = Set.of(UserRole.ADMIN, UserRole.SUPER_ADMIN);

    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Result<List<DemoAccountDTO>> findDemoAccounts() {
        if (!isDemoRuntime()) {
            return ResultFactory.ok(List.of());
        }

        String demoPassword = appProperties.getDemoAccounts().getPassword();

        if (demoPassword == null || demoPassword.isBlank()) {
            return ResultFactory.ok(List.of());
        }

        String emailPattern = appProperties.getDemoAccounts().getEmailPattern();

        if (emailPattern == null || emailPattern.isBlank()) {
            return ResultFactory.ok(List.of());
        }

        List<DemoAccountDTO> demoAccounts = userRepository.findVerifiedActiveDemoAccountCandidates(emailPattern)
                .stream()
                .filter(this::isNonPrivilegedUser)
                .filter(user -> user.getPassword() != null)
                .filter(user -> passwordEncoder.matches(demoPassword, user.getPassword()))
                .sorted(Comparator.comparing(User::getNickname, String.CASE_INSENSITIVE_ORDER))
                .map(user -> toDemoAccountDto(user, demoPassword))
                .toList();

        return ResultFactory.ok(demoAccounts);
    }

    private boolean isDemoRuntime() {
        return DEMO_RUNTIME_ENV.equalsIgnoreCase(appProperties.getRuntimeEnv());
    }

    private boolean isNonPrivilegedUser(User user) {
        Set<UserRole> roles = user.getRoles();

        return roles != null
                && roles.contains(UserRole.USER)
                && roles.stream().noneMatch(PRIVILEGED_ROLES::contains);
    }

    private DemoAccountDTO toDemoAccountDto(User user, String demoPassword) {
        return new DemoAccountDTO(
                user.getNickname(),
                user.getNickname(),
                user.getEmail(),
                demoPassword
        );
    }
}
