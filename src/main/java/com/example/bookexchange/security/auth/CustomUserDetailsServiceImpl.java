package com.example.bookexchange.security.auth;

import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl {

    private final UserRepository userRepository;

    public UserPrincipal loadUserByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .filter(foundUser -> foundUser.getDeletedAt() == null)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new UserPrincipal(user);
    }
}
