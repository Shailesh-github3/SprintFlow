package com.sprintflow.entity;

/**
 * What happened. Meaning depends on the entity type:
 * ISSUE -> CREATED / ASSIGNED / STATUS_CHANGED,
 * PROJECT_MEMBER -> INVITED / JOINED.
 */
public enum AuditAction {
    CREATED,
    ASSIGNED,
    STATUS_CHANGED,
    INVITED,
    JOINED
}
