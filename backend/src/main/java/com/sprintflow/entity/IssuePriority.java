package com.sprintflow.entity;

/**
 * Allowed priority levels for an issue. Matches the frontend contract
 * (IssueForm priority dropdown).
 */
public enum IssuePriority {
    LOW,
    MEDIUM,
    HIGH;

    public static IssuePriority from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Issue priority is required");
        }
        try {
            return IssuePriority.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid issue priority: " + raw + ". Allowed values: LOW, MEDIUM, HIGH");
        }
    }
}
