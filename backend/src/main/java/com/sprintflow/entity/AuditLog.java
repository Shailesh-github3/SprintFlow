package com.sprintflow.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Data;

@Entity
@Table(indexes = { @Index(name = "idx_audit_project", columnList = "project_project_id,created_at") })
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long auditId;

    @ManyToOne
    private User actor;

    @ManyToOne
    private Project project;

    @Enumerated(EnumType.STRING)
    private AuditEntityType entityType;

    private Long entityId;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    private String oldValue;
    private String newValue;

    private LocalDateTime createdAt;
}
