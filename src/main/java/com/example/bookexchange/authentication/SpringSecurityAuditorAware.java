package com.example.bookexchange.authentication;

import com.example.bookexchange.models.UserPrincipal;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringSecurityAuditorAware implements AuditorAware<Long> {

    @NullMarked
    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof UserPrincipal user) {
            return Optional.of(user.getId());
        }

        return Optional.empty();
    }
}
