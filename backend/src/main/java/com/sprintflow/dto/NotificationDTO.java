package com.sprintflow.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class NotificationDTO {
    private Long notificationId;
    private String title;
    private String message;
    private boolean readStatus;
    private String link;
    private LocalDateTime createdAt;
}
