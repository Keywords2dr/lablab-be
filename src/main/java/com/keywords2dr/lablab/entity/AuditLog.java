package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

// ... các import cũ giữ nguyên
@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false)
    private UUID logId;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "actor_username", updatable = false, length = 100)
    private String actorUsername;

    @Column(name = "actor_role", updatable = false, length = 50)
    private String actorRole;

    @Column(nullable = false, updatable = false, length = 50)
    private String action;

    @Column(name = "entity_name", nullable = false, updatable = false, length = 50)
    private String entityName;

    @Column(name = "record_id", updatable = false)
    private UUID recordId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", updatable = false)
    private String oldData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", updatable = false)
    private String newData;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}