package com.sprintflow.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AuditLogDTO {
    private Long auditId;
    private String actorName;
    private String actorEmail;
    private String entityType;
    private Long entityId;
    private String action;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;
    private String summary;
}
