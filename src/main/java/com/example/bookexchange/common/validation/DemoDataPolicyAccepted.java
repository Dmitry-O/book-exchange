package com.example.bookexchange.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = DemoDataPolicyAcceptedValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DemoDataPolicyAccepted {

    String message() default "{validation.demoDataPolicy.accepted}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
