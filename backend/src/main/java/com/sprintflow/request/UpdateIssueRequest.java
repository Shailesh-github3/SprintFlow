package com.sprintflow.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateIssueRequest {

    @NotBlank(message = "Issue title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @NotBlank(message = "Issue description is required")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Priority is required")
    @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "Priority must be one of LOW, MEDIUM, HIGH")
    private String priority;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @Pattern(regexp = "TODO|IN_PROGRESS|DONE", message = "Status must be one of TODO, IN_PROGRESS, DONE")
    private String status;

}
