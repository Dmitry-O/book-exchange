package com.example.bookexchange.common.validation;

import com.example.bookexchange.common.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataPolicyAcceptedValidatorTest {

    private AppProperties appProperties;
    private DemoDataPolicyAcceptedValidator validator;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        validator = new DemoDataPolicyAcceptedValidator(appProperties);
    }

    @Test
    void shouldRequireAcceptedValue_whenRuntimeIsDemo() {
        appProperties.setRuntimeEnv("demo");

        assertThat(validator.isValid(Boolean.TRUE, null)).isTrue();
        assertThat(validator.isValid(Boolean.FALSE, null)).isFalse();
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    void shouldIgnoreValue_whenRuntimeIsNotDemo() {
        appProperties.setRuntimeEnv("local");

        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid(Boolean.FALSE, null)).isTrue();
    }
}
