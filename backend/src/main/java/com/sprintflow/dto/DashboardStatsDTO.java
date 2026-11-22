package com.sprintflow.dto;

public record DashboardStatsDTO(
    long totalProjects,
    long totalIssues,
    long todoIssues,
    long inProgressIssues,
    long completedIssues,
    long overdueIssues,
    double completionPercentage
) {}
