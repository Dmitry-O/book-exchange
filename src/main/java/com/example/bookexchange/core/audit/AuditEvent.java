package com.example.bookexchange.core.audit;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Map;

@Data
@Builder
public class AuditEvent {

    private String action;

    private AuditResult result;

    private Long actorId;

    private String actorEmail;

    private String reason;

    @Singular("detail")
    private Map<String, Object> details;
}
