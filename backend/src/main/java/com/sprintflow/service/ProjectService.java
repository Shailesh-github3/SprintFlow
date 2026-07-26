package com.sprintflow.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintflow.entity.Chat;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.repository.ProjectRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final ChatService chatService;

    public ProjectService(ProjectRepository projectRepository, UserService userService, ChatService chatService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.chatService = chatService;
    }

    @Transactional
    public Project createProject(Project project, User projectOwner) {
        Project newProject = new Project();
        newProject.setProjectName(project.getProjectName());
        newProject.setProjectOwner(projectOwner);
        newProject.setProjectDescription(project.getProjectDescription());
        newProject.setTags(project.getTags());
        newProject.setCategory(project.getCategory());
        newProject.setProjectMembers(project.getProjectMembers());
        if (newProject.getProjectMembers() == null) {
            newProject.setProjectMembers(new java.util.ArrayList<>());
        }
        newProject.getProjectMembers().add(projectOwner); // Add the project owner to the project members
        
        Chat chat = new Chat();
        chat.setProject(newProject);
        newProject.setChat(chat);

        return projectRepository.save(newProject);
    }

    @Transactional(readOnly = true)
    public Page<Project> getProjectByTeamMember(User teamMember, String category, String tag, Pageable pageable) throws Exception {
        return projectRepository.findProjectsForMemberWithFilters(teamMember, category, tag, pageable);
    }

    @Transactional(readOnly = true)
    public Project getProjectById(Long projectId) throws Exception {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new com.sprintflow.exception.ResourceNotFoundException("Project not found with id: " + projectId));
    }

    @Transactional(readOnly = true)
    public void ensureProjectOwner(Long projectId, String userEmail) throws Exception {
        Project project = getProjectById(projectId);
        User actor = userService.findByEmail(userEmail);
        if (project.getProjectOwner() == null || !project.getProjectOwner().getUserId().equals(actor.getUserId())) {
            throw new SecurityException("User is not authorized for this project");
        }
    }

    @Transactional(readOnly = true)
    public void ensureProjectMemberOrOwner(Long projectId, String userEmail) throws Exception {
        Project project = getProjectById(projectId);
        User actor = userService.findByEmail(userEmail);
        boolean isOwner = project.getProjectOwner() != null && project.getProjectOwner().getUserId().equals(actor.getUserId());
        boolean isMember = project.getProjectMembers() != null && project.getProjectMembers().contains(actor);
        if (!isOwner && !isMember) {
            throw new SecurityException("User is not authorized for this project");
        }
    }

    @Transactional
    public void deleteProject(Long projectId) throws Exception {
        Project project = getProjectById(projectId);
        projectRepository.delete(project);
    }

    @Transactional
    public Project updateProject(Long projectId, Project updatedProject) throws Exception {
        Project existingProject = getProjectById(projectId);
        existingProject.setProjectName(updatedProject.getProjectName());
        existingProject.setProjectDescription(updatedProject.getProjectDescription());
        existingProject.setCategory(updatedProject.getCategory());
        existingProject.setTags(updatedProject.getTags());
        return projectRepository.save(existingProject);
    }

    @Transactional
    public void addTeamMember(Long projectId, Long userId) throws Exception {
        Project project = getProjectById(projectId);
        User user = userService.getUserByUserId(userId);
        if (!project.getProjectMembers().contains(user)) {
            project.getProjectMembers().add(user);
            projectRepository.save(project);
        } else {
            throw new Exception("User is already a member of the project.");
        }
    }

    @Transactional
    public void removeTeamMember(Long projectId, Long userId) throws Exception {
        Project project = getProjectById(projectId);
        User user = userService.getUserByUserId(userId);
        if (project.getProjectMembers().contains(user)) {
            project.getProjectMembers().remove(user);
            projectRepository.save(project);
        } else {
            throw new Exception("User is not a member of the project.");
        }
    }

    @Transactional(readOnly = true)
    public Chat getChatByProjectId(Long projectId) throws Exception {
        Project project = getProjectById(projectId);
        if (project.getChat() == null) {
            throw new Exception("Chat not found for project with id: " + projectId);
        }
        return project.getChat();
    }

    @Transactional(readOnly = true)
    public List<Project> searchProjects(String keyword, User user) { 
        List<Project> projects = projectRepository.findByProjectNameContainingAndProjectMembersContaining(keyword, user);
        return projects;
    }

}