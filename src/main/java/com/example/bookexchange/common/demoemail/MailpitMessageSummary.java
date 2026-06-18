package com.example.bookexchange.common.demoemail;

import java.time.Instant;
import java.util.List;

record MailpitMessageSummary(
        String id,
        String from,
        List<String> to,
        String subject,
        String snippet,
        Instant createdAt
) { }
