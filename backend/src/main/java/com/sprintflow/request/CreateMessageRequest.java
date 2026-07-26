package com.sprintflow.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateMessageRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;
    @NotBlank(message = "Message text is required")
    private String content;

}
