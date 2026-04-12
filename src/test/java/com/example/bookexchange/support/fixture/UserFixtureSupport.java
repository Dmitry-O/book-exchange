package com.example.bookexchange.support.fixture;

import com.example.bookexchange.common.result.Failure;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.Success;
import com.example.bookexchange.support.TestUserStrings;
import com.example.bookexchange.security.auth.UserPrincipal;
import com.example.bookexchange.user.dto.SupportedLocalesDTO;
import com.example.bookexchange.user.dto.UserCreateDTO;
import com.example.bookexchange.user.mapper.UserMapper;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.user.service.UserService;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;

@RequiredArgsConstructor
public class UserFixtureSupport {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public User createUser(Integer userNumberParam) {
        return createUser(userNumberParam, true, UserRole.USER);
    }

    public User createUser(Integer userNumberParam, boolean emailVerified) {
        return createUser(userNumberParam, emailVerified, UserRole.USER);
    }

    public User createAdmin(Integer userNumberParam) {
        return createUser(userNumberParam, true, UserRole.USER, UserRole.ADMIN);
    }

    public User createSuperAdmin(Integer userNumberParam) {
        return createUser(userNumberParam, true, UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    }

    public User createUser(Integer userNumberParam, boolean emailVerified, UserRole... roles) {
        Integer userNumber = Optional.ofNullable(userNumberParam).orElse(1);

        UserCreateDTO userCreateDTO = buildUserCreateDTO(userNumber);

        String email = userCreateDTO.getEmail();
        String nickname = userCreateDTO.getNickname();

        userRepository.findByEmail(email).ifPresent(user -> {
            throw new EntityExistsException("Es gibt bereits ein Benutzer mit diesem Email. Wählen Sie bitte ein anderes");
        });

        userRepository.findByNickname(nickname).ifPresent(user -> {
            throw new EntityExistsException("Es gibt bereits ein Benutzer mit diesem Nickname. Wählen Sie bitte einen anderen");
        });

        userCreateDTO.setPassword(passwordEncoder.encode(userCreateDTO.getPassword()));

        User user = userMapper.userCreateDtoToUser(userCreateDTO);
        for (UserRole role : roles) {
            user.addRole(role);
        }
        user.setEmailVerified(emailVerified);

        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        Long version = userRepository.findById(userId).orElseThrow().getVersion();
        unwrap(userService.deleteUser(userId, version));
    }

    public static RequestPostProcessor testUser(User user) {
        return request -> {
            UserPrincipal principal = new UserPrincipal(user);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);

            return SecurityMockMvcRequestPostProcessors
                    .securityContext(context)
                    .postProcessRequest(request);
        };
    }

    public UserCreateDTO buildUserCreateDTO(Integer userNumberParam) {
        Integer userNumber = Optional.ofNullable(userNumberParam).orElse(1);

        return UserCreateDTO.builder()
                .email(TestUserStrings.email(userNumber))
                .password("Password1!")
                .nickname(TestUserStrings.nickname(userNumber))
                .locale(SupportedLocalesDTO.EN.getProperty())
                .build();
    }

    private <T> T unwrap(Result<T> result) {
        if (result instanceof Success<T> success) {
            return success.body();
        }

        if (result instanceof Failure<T> failure) {
            throw new IllegalStateException(
                    "Expected success result, but got failure: "
                            + failure.messageKey()
                            + " (" + failure.status() + ")"
            );
        }

        throw new IllegalStateException("Unknown result type: " + result.getClass().getName());
    }
}
