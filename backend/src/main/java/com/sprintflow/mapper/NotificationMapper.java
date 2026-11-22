package com.sprintflow.mapper;

import org.mapstruct.Mapper;

import com.sprintflow.dto.NotificationDTO;
import com.sprintflow.entity.Notification;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationDTO toDto(Notification notification);
}
