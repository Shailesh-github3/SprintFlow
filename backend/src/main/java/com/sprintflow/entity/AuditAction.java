package com.sprintflow.entity;

/**
 * What happened. Meaning depends on the entity type:
 * ISSUE -> CREATED / UPDATED / DELETED / ASSIGNED / STATUS_CHANGED,
 * PROJECT_MEMBER -> INVITED / JOINED / REMOVED / LEFT.
 */
public enum AuditAction {
    CREATED,
    UPDATED,
    DELETED,
    ASSIGNED,
    STATUS_CHANGED,
    INVITED,
    JOINED,
    REMOVED,
    LEFT
}
