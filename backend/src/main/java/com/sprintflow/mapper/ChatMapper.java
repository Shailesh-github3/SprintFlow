package com.sprintflow.mapper;
import org.mapstruct.Mapper;
import com.sprintflow.entity.Chat;
import com.sprintflow.dto.ChatDTO;

@Mapper(componentModel = "spring")
public interface ChatMapper {
    ChatDTO toDto(Chat chat);
}
