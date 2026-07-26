package com.sprintflow.request;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueRequest {

    @NotBlank(message = "Issue title is required")
    private String title;
    @NotBlank
    private String description;
    @NotBlank
    private String status;
    @NotNull(message = "Project ID is required")
    private Long projectId;
    @NotBlank
    private String priority;
    @NotNull
    private LocalDate dueDate; 

}
