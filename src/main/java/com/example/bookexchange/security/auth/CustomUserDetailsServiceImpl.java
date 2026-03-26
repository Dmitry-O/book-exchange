package com.example.bookexchange.security.auth;

import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl {

    private final UserRepository userRepository;

    public UserPrincipal loadUserByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        return new UserPrincipal(user);
    }
}
