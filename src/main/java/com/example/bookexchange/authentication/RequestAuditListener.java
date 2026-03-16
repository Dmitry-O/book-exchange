package com.example.bookexchange.authentication;

import com.example.bookexchange.models.AuditableEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class RequestAuditListener {

    @PrePersist
    public void setCreatedRequestId(AuditableEntity entity) {
        String requestId = RequestContext.getRequestId();

        if (requestId != null) {
            entity.setCreatedRequestId(requestId);
            entity.setUpdatedRequestId(requestId);
        }
    }

    @PreUpdate
    public void setUpdatedRequestId(AuditableEntity entity) {

        String requestId = RequestContext.getRequestId();

        if (requestId != null) {
            entity.setUpdatedRequestId(requestId);
        }
    }
}
