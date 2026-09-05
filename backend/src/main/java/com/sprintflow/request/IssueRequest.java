package com.sprintflow.request;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueRequest {

    @NotBlank(message = "Issue title is required")
    private String title;
    @NotBlank
    private String description;
    @NotBlank
    @Pattern(regexp = "TODO|IN_PROGRESS|DONE", message = "Status must be one of TODO, IN_PROGRESS, DONE")
    private String status;
    @NotNull(message = "Project ID is required")
    private Long projectId;
    @NotBlank
    @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "Priority must be one of LOW, MEDIUM, HIGH")
    private String priority;
    @NotNull
    private LocalDate dueDate; 

}
