package com.example.bookexchange.authentication;

import com.example.bookexchange.models.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

public class UserLocaleResolver implements LocaleResolver {

    @NullMarked
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal user && user.getLocale() != null) {
            return Locale.forLanguageTag(user.getLocale());
        }

        Locale headerLocale = request.getLocale();

        if (headerLocale != null) {
            return headerLocale;
        }

        return Locale.ENGLISH;
    }

    @Override
    public void setLocale(@NonNull HttpServletRequest request, HttpServletResponse response, Locale locale) { }
}
