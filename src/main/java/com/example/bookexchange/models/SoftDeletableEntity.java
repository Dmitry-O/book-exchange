package com.example.bookexchange.models;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
@FilterDef(name = "deletedFilter")
@Filter(name = "deletedFilter", condition = "deleted_at IS NULL")
public class SoftDeletableEntity extends AuditableEntity {

    @Column
    private Instant deletedAt;
}
