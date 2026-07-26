package com.sprintflow.dto;
import lombok.Data;
import java.time.LocalDate;
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
    private Long projectId;
}
