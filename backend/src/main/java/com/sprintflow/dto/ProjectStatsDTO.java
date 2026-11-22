package com.sprintflow.dto;

public record ProjectStatsDTO(
    Long projectId,
    long totalIssues,
    long todoIssues,
    long inProgressIssues,
    long completedIssues,
    long overdueIssues,
    double completionPercentage
) {}
