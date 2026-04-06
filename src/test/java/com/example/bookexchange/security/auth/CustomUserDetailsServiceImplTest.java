package com.example.bookexchange.security.auth;

import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsServiceImpl userDetailsService;

    @Test
    void shouldReturnPrincipal_whenLoadUserByUserIdFindsActiveUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserPrincipal principal = userDetailsService.loadUserByUserId(user.getId());

        assertThat(principal.getId()).isEqualTo(user.getId());
        assertThat(principal.getUsername()).isEqualTo(user.getEmail());
    }

    @Test
    void shouldThrow_whenLoadUserByUserIdFindsDeletedUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        user.setDeletedAt(Instant.now());

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userDetailsService.loadUserByUserId(user.getId()))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }
}
