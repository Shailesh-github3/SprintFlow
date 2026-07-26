package com.sprintflow.dto;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageDTO {
    private Long messageId;
    private String messageText;
    private LocalDateTime createdAt;
    private UserDTO sender;
}
