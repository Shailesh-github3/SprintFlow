package com.sprintflow.mapper;
import org.mapstruct.Mapper;
import com.sprintflow.entity.User;
import com.sprintflow.dto.UserDTO;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDto(User user);
}
