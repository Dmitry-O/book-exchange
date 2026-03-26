package com.example.bookexchange.common.audit.model;

import com.example.bookexchange.security.context.RequestAuditListener;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
@EntityListeners({ AuditingEntityListener.class, RequestAuditListener.class })
@Getter
@Setter
public abstract class AuditableEntity implements VersionedEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private Long createdBy;

    @LastModifiedBy
    private Long updatedBy;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(updatable = false)
    private String createdRequestId;

    @Column
    private String updatedRequestId;
}
