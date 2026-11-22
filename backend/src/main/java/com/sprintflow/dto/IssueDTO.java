package com.sprintflow.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class IssueDTO {
    private Long issueId;
    private String issueTitle;
    private String issueDescription;
    private String issueStatus;
    private String priority;
    private LocalDate dueDate;
    private List<String> tags;
    private UserDTO assignedUser;
    private UserDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long projectId;
}
