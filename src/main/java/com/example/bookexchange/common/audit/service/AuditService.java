package com.example.bookexchange.common.audit.service;

import com.example.bookexchange.common.audit.model.AuditEvent;

public interface AuditService {

    void log(AuditEvent event);
}
