package com.sprintflow.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    private String projectName;

    @NotBlank(message = "Project description is required")
    private String projectDescription;

    @NotBlank(message = "Category is required")
    private String category;

    private List<String> tags;
}