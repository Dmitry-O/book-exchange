package com.example.bookexchange.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPublicationYearValidator.class)
@Documented
public @interface ValidPublicationYear {

    String message() default "{validation.publicationYear.valid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
