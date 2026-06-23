package com.example.bookexchange.common.demoaccounts;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public demo account credentials for the portfolio demo login picker")
public record DemoAccountDTO(
        @Schema(example = "demo_reader")
        String label,

        @Schema(example = "demo_reader")
        String nickname,

        @Schema(example = "reader.demo@example.com")
        String email,

        @Schema(example = "DemoPassword123!")
        String password
) {
}
