package com.example.bookexchange.services;

import com.example.bookexchange.models.User;
import com.example.bookexchange.models.UserPrincipal;
import com.example.bookexchange.repositories.UserRepository;
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
