package com.sprintflow.mapper;
import org.mapstruct.Mapper;
import com.sprintflow.entity.Message;
import com.sprintflow.dto.MessageDTO;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface MessageMapper {
    MessageDTO toDto(Message message);
}
