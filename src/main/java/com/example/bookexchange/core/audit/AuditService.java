package com.example.bookexchange.core.audit;

public interface AuditService {

    void log(AuditEvent event);
}
