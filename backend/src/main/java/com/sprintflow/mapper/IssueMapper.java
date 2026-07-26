package com.sprintflow.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.sprintflow.entity.Issue;
import com.sprintflow.dto.IssueDTO;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface IssueMapper {
    @Mapping(target = "projectId", source = "project.projectId")
    IssueDTO toDto(Issue issue);
}
