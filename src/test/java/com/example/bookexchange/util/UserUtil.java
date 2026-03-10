package com.example.bookexchange.util;

import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.exception.EntityExistsException;
import com.example.bookexchange.mappers.UserMapper;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class UserUtil {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public User createUser(Integer userNumberParam) {
        Integer userNumber = Optional.ofNullable(userNumberParam).orElse(1);

        UserCreateDTO userCreateDTO = UserCreateDTO.builder()
                .email("user" + userNumber + "@test.com")
                .password("password")
                .nickname("user" + userNumber)
                .build();

        String email = userCreateDTO.getEmail();
        String nickname = userCreateDTO.getNickname();

        userRepository.findByEmail(email).ifPresent(user -> {
            throw new EntityExistsException("Es gibt bereits ein Benutzer mit diesem Email. Wählen Sie bitte ein anderes");
        });

        userRepository.findByNickname(nickname).ifPresent(user -> {
            throw new EntityExistsException("Es gibt bereits ein Benutzer mit diesem Nickname. Wählen Sie bitte einen anderen");
        });

        userCreateDTO.setPassword(passwordEncoder.encode(userCreateDTO.getPassword()));

        return userRepository.save(userMapper.userDtoToUser(userCreateDTO));
    }

    public void deleteUser(Long userId) {
        userService.deleteUser(userId);
    }

    public static RequestPostProcessor testUser(User user) {
        return request -> {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);

            return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .securityContext(context)
                    .postProcessRequest(request);
        };
    }
}
