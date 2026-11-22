package com.sprintflow.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintflow.dto.ProjectStatsDTO;
import com.sprintflow.entity.AuditAction;
import com.sprintflow.entity.AuditEntityType;
import com.sprintflow.entity.Chat;
import com.sprintflow.entity.Issue;
import com.sprintflow.entity.PlanType;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.Subscription;
import com.sprintflow.entity.User;
import com.sprintflow.exception.ResourceNotFoundException;
import com.sprintflow.repository.IssueRepository;
import com.sprintflow.repository.ProjectRepository;

@Service
public class ProjectService {

    public static final int FREE_PLAN_PROJECT_LIMIT = 3;

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final IssueRepository issueRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public ProjectService(ProjectRepository projectRepository, UserService userService, SubscriptionService subscriptionService) {
        this(projectRepository, userService, subscriptionService, null, null, null);
    }

    @Autowired
    public ProjectService(ProjectRepository projectRepository, UserService userService,
                          SubscriptionService subscriptionService,
                          @Autowired(required = false) IssueRepository issueRepository,
                          @Autowired(required = false) AuditService auditService,
                          @Autowired(required = false) NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.issueRepository = issueRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Project createProject(Project project, User projectOwner) {
        if (projectOwner != null && projectOwner.getUserId() != null && subscriptionService != null) {
            try {
                Subscription subscription = subscriptionService.getSubscriptionByUserId(projectOwner.getUserId());
                if (subscription != null && subscription.getPlanType() == PlanType.FREE) {
                    long count = projectRepository.countByProjectOwner(projectOwner);
                    if (count >= FREE_PLAN_PROJECT_LIMIT) {
                        throw new SecurityException("Free plan project limit (" + FREE_PLAN_PROJECT_LIMIT + " projects) reached. Please upgrade to create more projects.");
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // Allow creation if subscription record is absent (e.g. legacy/testing)
            }
        }

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
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
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
            throw new IllegalArgumentException("User is already a member of the project.");
        }
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, String actorEmail) throws Exception {
        ensureProjectOwner(projectId, actorEmail);
        Project project = getProjectById(projectId);
        User memberToRemove = userService.getUserByUserId(userId);

        if (project.getProjectOwner() != null && project.getProjectOwner().getUserId().equals(memberToRemove.getUserId())) {
            throw new IllegalArgumentException("Cannot remove project owner from project members");
        }

        if (!project.getProjectMembers().contains(memberToRemove)) {
            throw new IllegalArgumentException("User is not a member of this project");
        }

        project.getProjectMembers().remove(memberToRemove);
        projectRepository.save(project);

        // Unassign any issues assigned to this member in this project
        if (issueRepository != null) {
            List<Issue> assignedIssues = issueRepository.findByProject_ProjectIdAndAssignedUser_UserId(projectId, userId);
            for (Issue issue : assignedIssues) {
                issue.setAssignedUser(null);
                issueRepository.save(issue);
            }
        }

        if (auditService != null) {
            auditService.log(actorEmail, projectId, AuditEntityType.PROJECT_MEMBER,
                    userId, AuditAction.REMOVED, memberToRemove.getFullName(), "Removed by owner");
        }

        if (notificationService != null) {
            notificationService.createNotification(
                    memberToRemove,
                    "Removed from Project",
                    "You were removed from project: " + project.getProjectName(),
                    "/dashboard"
            );
        }
    }

    @Transactional
    public void leaveProject(Long projectId, String userEmail) throws Exception {
        Project project = getProjectById(projectId);
        User actor = userService.findByEmail(userEmail);

        if (project.getProjectOwner() != null && project.getProjectOwner().getUserId().equals(actor.getUserId())) {
            throw new IllegalArgumentException("Project owner cannot leave the project. Please transfer ownership or delete the project.");
        }

        if (!project.getProjectMembers().contains(actor)) {
            throw new IllegalArgumentException("User is not a member of this project");
        }

        project.getProjectMembers().remove(actor);
        projectRepository.save(project);

        // Unassign issues assigned to actor in this project
        if (issueRepository != null) {
            List<Issue> assignedIssues = issueRepository.findByProject_ProjectIdAndAssignedUser_UserId(projectId, actor.getUserId());
            for (Issue issue : assignedIssues) {
                issue.setAssignedUser(null);
                issueRepository.save(issue);
            }
        }

        if (auditService != null) {
            auditService.log(userEmail, projectId, AuditEntityType.PROJECT_MEMBER,
                    actor.getUserId(), AuditAction.LEFT, actor.getFullName(), "Left project");
        }

        if (notificationService != null && project.getProjectOwner() != null) {
            notificationService.createNotification(
                    project.getProjectOwner(),
                    "Member Left Project",
                    actor.getFullName() + " left project: " + project.getProjectName(),
                    "/project/" + projectId
            );
        }
    }

    @Transactional(readOnly = true)
    public ProjectStatsDTO getProjectStats(Long projectId, String userEmail) throws Exception {
        ensureProjectMemberOrOwner(projectId, userEmail);

        long totalIssues = issueRepository != null ? issueRepository.countTotalIssuesByProject(projectId) : 0;
        long todoIssues = issueRepository != null ? issueRepository.countIssuesByProjectAndStatus(projectId, "TODO") : 0;
        long inProgressIssues = issueRepository != null ? issueRepository.countIssuesByProjectAndStatus(projectId, "IN_PROGRESS") : 0;
        long doneIssues = issueRepository != null ? issueRepository.countIssuesByProjectAndStatus(projectId, "DONE") : 0;
        long overdueIssues = issueRepository != null ? issueRepository.countOverdueIssuesByProject(projectId) : 0;
        double completionPercentage = totalIssues > 0 ? ((double) doneIssues / totalIssues) * 100.0 : 0.0;

        return new ProjectStatsDTO(
                projectId,
                totalIssues,
                todoIssues,
                inProgressIssues,
                doneIssues,
                overdueIssues,
                Math.round(completionPercentage * 10.0) / 10.0
        );
    }

    @Transactional(readOnly = true)
    public Chat getChatByProjectId(Long projectId) throws Exception {
        Project project = getProjectById(projectId);
        if (project.getChat() == null) {
            throw new ResourceNotFoundException("Chat not found for project with id: " + projectId);
        }
        return project.getChat();
    }

    @Transactional(readOnly = true)
    public Page<Project> searchProjects(String keyword, User user, Pageable pageable) {
        return projectRepository.findByProjectNameContainingAndProjectMembersContaining(keyword, user, pageable);
    }

}
