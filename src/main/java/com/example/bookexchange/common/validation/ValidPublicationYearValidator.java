package com.example.bookexchange.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Year;

public class ValidPublicationYearValidator implements ConstraintValidator<ValidPublicationYear, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        int currentYear = Year.now().getValue();
        return value >= 1 && value <= currentYear;
    }
}
