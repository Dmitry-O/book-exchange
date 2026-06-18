package com.example.bookexchange.common.demoemail;

import java.time.Instant;

public record DemoEmailSandboxSessionDTO(
        String sandboxId,
        Instant expiresAt,
        boolean enabled
) { }
