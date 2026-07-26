package com.sprintflow.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProjectDTO {
    private Long projectId;
    private String projectName;
    private String projectDescription;
    private String category;
    private List<String> tags;
    private UserDTO projectOwner;
    private List<UserDTO> projectMembers;
    private ChatDTO chat;
}