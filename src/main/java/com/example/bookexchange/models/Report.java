package com.example.bookexchange.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Builder
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    @NotNull
    private Long targetId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    @NotBlank
    private String comment;

    @CreationTimestamp
    private Instant createdAt;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.OPEN;

    @ManyToOne
    @JsonBackReference
    private User reporter;
}
