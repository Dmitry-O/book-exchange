package com.example.bookexchange.common.demoemail;

import java.time.Instant;
import java.util.List;

public record DemoEmailMessageDTO(
        String id,
        String from,
        List<String> to,
        String subject,
        String snippet,
        Instant createdAt,
        String html,
        String text
) { }
