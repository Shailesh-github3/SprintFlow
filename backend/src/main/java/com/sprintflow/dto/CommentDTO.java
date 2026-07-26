package com.sprintflow.dto;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private Long commentId;
    private String commentText;
    private LocalDateTime createdAt;
    private UserDTO createdBy;
    private Long issueId;
}
