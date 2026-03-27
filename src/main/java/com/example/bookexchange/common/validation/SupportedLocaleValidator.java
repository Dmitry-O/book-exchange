package com.example.bookexchange.common.validation;

import com.example.bookexchange.user.dto.SupportedLocalesDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SupportedLocaleValidator implements ConstraintValidator<SupportedLocale, String> {

    private static final Set<String> SUPPORTED_LOCALES = Arrays.stream(SupportedLocalesDTO.values())
            .map(SupportedLocalesDTO::getProperty)
            .collect(Collectors.toUnmodifiableSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value.isBlank()) {
            return false;
        }

        return SUPPORTED_LOCALES.contains(value.toLowerCase());
    }
}
