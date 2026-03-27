package com.example.bookexchange.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Base64;

public class Base64ImageValidator implements ConstraintValidator<Base64Image, String> {

    private int maxLength;

    @Override
    public void initialize(Base64Image annotation) {
        this.maxLength = annotation.maxLength();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        if (value.length() > maxLength) {
            return false;
        }

        String normalized = value;
        int commaIndex = value.indexOf(',');

        if (value.startsWith("data:image/") && commaIndex > 0) {
            normalized = value.substring(commaIndex + 1);
        }

        try {
            Base64.getDecoder().decode(normalized);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
