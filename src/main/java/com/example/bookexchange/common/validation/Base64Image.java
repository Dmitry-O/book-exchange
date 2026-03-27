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
@Constraint(validatedBy = Base64ImageValidator.class)
@Documented
public @interface Base64Image {

    String message() default "{validation.image.base64}";

    int maxLength() default 255;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
