package com.example.bookexchange.common.validation;

import com.example.bookexchange.common.config.AppProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoDataPolicyAcceptedValidator implements ConstraintValidator<DemoDataPolicyAccepted, Boolean> {

    private static final String DEMO_RUNTIME_ENV = "demo";

    private final AppProperties appProperties;

    @Override
    public boolean isValid(Boolean value, ConstraintValidatorContext context) {
        if (!DEMO_RUNTIME_ENV.equalsIgnoreCase(appProperties.getRuntimeEnv())) {
            return true;
        }

        return Boolean.TRUE.equals(value);
    }
}
