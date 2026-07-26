package com.sprintflow.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCommentRequest {

    @NotNull(message = "Issue ID is required")
    private Long issueId;
    @NotBlank(message = "Content is required")
    private String content;

}
