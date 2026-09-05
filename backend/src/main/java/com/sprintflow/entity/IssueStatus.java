package com.sprintflow.entity;

/**
 * Allowed lifecycle states for an issue. Matches the frontend contract
 * (IssueForm / IssueList status dropdowns).
 */
public enum IssueStatus {
    TODO,
    IN_PROGRESS,
    DONE;

    public static IssueStatus from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Issue status is required");
        }
        try {
            return IssueStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid issue status: " + raw + ". Allowed values: TODO, IN_PROGRESS, DONE");
        }
    }
}
