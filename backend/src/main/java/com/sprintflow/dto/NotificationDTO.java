package com.sprintflow.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private Long notificationId;
    private String title;
    private String message;
    private boolean readStatus;
    private String link;
    private LocalDateTime createdAt;
}
