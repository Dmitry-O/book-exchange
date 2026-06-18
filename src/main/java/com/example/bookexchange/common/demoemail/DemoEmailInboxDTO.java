package com.example.bookexchange.common.demoemail;

import java.time.Instant;
import java.util.List;

public record DemoEmailInboxDTO(
        String sandboxId,
        Instant expiresAt,
        List<DemoEmailMessageDTO> messages
) { }
