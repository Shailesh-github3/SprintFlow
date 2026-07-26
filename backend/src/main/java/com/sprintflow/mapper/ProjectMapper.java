package com.sprintflow.mapper;
import org.mapstruct.Mapper;
import com.sprintflow.entity.Project;
import com.sprintflow.dto.ProjectDTO;

@Mapper(componentModel = "spring", uses = {UserMapper.class, ChatMapper.class})
public interface ProjectMapper {
    ProjectDTO toDto(Project project);
}
