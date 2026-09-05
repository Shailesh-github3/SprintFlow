package com.sprintflow.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.sprintflow.entity.Issue;
import com.sprintflow.dto.IssueDTO;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface IssueMapper {
    @Mapping(target = "projectId", source = "project.projectId")
    IssueDTO toDto(Issue issue);

    @Mapping(target = "project", source = "projectId")
    Issue toEntity(IssueDTO issueDTO);

    default com.sprintflow.entity.Project map(Long projectId) {
        if (projectId == null) {
            return null;
        }
        com.sprintflow.entity.Project project = new com.sprintflow.entity.Project();
        project.setProjectId(projectId);
        return project;
    }
}
