package com.sprintflow.service;

import java.util.Optional;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintflow.entity.Issue;
import com.sprintflow.entity.AuditAction;
import com.sprintflow.entity.AuditEntityType;
import com.sprintflow.entity.IssuePriority;
import com.sprintflow.entity.IssueStatus;
import com.sprintflow.repository.IssueRepository;
import com.sprintflow.request.IssueRequest;
import com.sprintflow.request.UpdateIssueRequest;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.exception.ResourceNotFoundException;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public IssueService(IssueRepository issueRepository, ProjectService projectService, UserService userService, AuditService auditService) {
        this(issueRepository, projectService, userService, auditService, null);
    }

    @Autowired
    public IssueService(IssueRepository issueRepository, ProjectService projectService, UserService userService,
                        AuditService auditService, @Autowired(required = false) NotificationService notificationService) {
        this.issueRepository = issueRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Issue createIssue(IssueRequest issue) throws Exception {
        Project project = projectService.getProjectById(issue.getProjectId());
        Issue newIssue = new Issue();
        newIssue.setIssueTitle(issue.getTitle());
        newIssue.setIssueDescription(issue.getDescription());
        newIssue.setIssueStatus(IssueStatus.from(issue.getStatus()).name());
        newIssue.setPriority(IssuePriority.from(issue.getPriority()).name());
        newIssue.setDueDate(issue.getDueDate());
        newIssue.setProject(project);
        return issueRepository.save(newIssue);
    }

    @Transactional
    public Issue createIssue(IssueRequest issue, String actorEmail) throws Exception {
        Project project = projectService.ensureProjectMemberOrOwner(issue.getProjectId(), actorEmail);
        if (project == null) {
            project = projectService.getProjectById(issue.getProjectId());
        }
        User creator = userService.findByEmail(actorEmail);

        Issue newIssue = new Issue();
        newIssue.setIssueTitle(issue.getTitle());
        newIssue.setIssueDescription(issue.getDescription());
        newIssue.setIssueStatus(IssueStatus.from(issue.getStatus()).name());
        newIssue.setPriority(IssuePriority.from(issue.getPriority()).name());
        newIssue.setDueDate(issue.getDueDate());
        newIssue.setProject(project);
        newIssue.setCreatedBy(creator);

        Issue created = issueRepository.save(newIssue);
        auditService.log(actorEmail, issue.getProjectId(), AuditEntityType.ISSUE,
                created.getIssueId(), AuditAction.CREATED, null, null);
        return created;
    }

    @Transactional(readOnly = true)
    public Issue getIssueById(Long issueId) throws Exception {
        Long resolvedIssueId = Objects.requireNonNull(issueId, "issueId must not be null");
        Optional<Issue> issue = issueRepository.findById(resolvedIssueId);
        if (!issue.isPresent()) {
            throw new ResourceNotFoundException("Issue not found with id: " + issueId);
        }
        return issue.get();
    }

    @Transactional(readOnly = true)
    public Page<Issue> getIssuesByProjectId(Long projectId, Pageable pageable) {
        return issueRepository.findByProject_ProjectId(projectId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Issue> getIssuesByProjectId(Long projectId, String status, String priority, Long assigneeId, String search, Pageable pageable) {
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim() : null;
        String cleanPriority = (priority != null && !priority.isBlank()) ? priority.trim() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return issueRepository.findByProjectWithFilters(projectId, cleanStatus, cleanPriority, assigneeId, cleanSearch, pageable);
    }

    @Transactional
    public void deleteIssue(Long issueId) throws Exception {
        Long resolvedIssueId = Objects.requireNonNull(issueId, "issueId must not be null");
        Optional<Issue> issue = issueRepository.findById(resolvedIssueId);
        if (!issue.isPresent()) {
            throw new ResourceNotFoundException("Issue not found with id: " + resolvedIssueId);
        }
        issueRepository.deleteById(resolvedIssueId);
    }

    @Transactional
    public void deleteIssue(Long issueId, String actorEmail) throws Exception {
        Issue issue = getIssueById(issueId);
        Project project = issue.getProject();

        boolean isOwner = project.getProjectOwner() != null && project.getProjectOwner().getEmail().equalsIgnoreCase(actorEmail);
        boolean isCreator = issue.getCreatedBy() != null && issue.getCreatedBy().getEmail().equalsIgnoreCase(actorEmail);

        if (!isOwner && !isCreator) {
            throw new SecurityException("Only the project owner or the issue creator can delete this issue");
        }

        auditService.log(actorEmail, project.getProjectId(), AuditEntityType.ISSUE,
                issueId, AuditAction.DELETED, issue.getIssueTitle(), null);
        issueRepository.delete(issue);
    }

    @Transactional
    public Issue updateIssue(Long issueId, UpdateIssueRequest request, String actorEmail) throws Exception {
        Issue issue = getIssueById(issueId);
        projectService.ensureProjectMemberOrOwner(issue.getProject().getProjectId(), actorEmail);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            issue.setIssueTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            issue.setIssueDescription(request.getDescription().trim());
        }
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            issue.setPriority(IssuePriority.from(request.getPriority()).name());
        }
        if (request.getDueDate() != null) {
            issue.setDueDate(request.getDueDate());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            issue.setIssueStatus(IssueStatus.from(request.getStatus()).name());
        }

        Issue updated = issueRepository.save(issue);
        auditService.log(actorEmail, issue.getProject().getProjectId(), AuditEntityType.ISSUE,
                issueId, AuditAction.UPDATED, null, null);
        return updated;
    }

    @Transactional
    public Issue addUserToIssue(Long issueId, Long userId) throws Exception {
        User user = userService.getUserByUserId(userId);
        Issue issue = getIssueById(issueId);
        issue.setAssignedUser(user);
        return issueRepository.save(issue);
    }

    @Transactional
    public Issue addUserToIssue(Long issueId, Long userId, String actorEmail) throws Exception {
        Issue issue = getIssueById(issueId);
        projectService.ensureProjectOwner(issue.getProject().getProjectId(), actorEmail);
        User user = userService.getUserByUserId(userId);

        Project project = issue.getProject();
        boolean isOwner = project.getProjectOwner() != null && project.getProjectOwner().getUserId().equals(userId);
        boolean isMember = project.getProjectMembers() != null && project.getProjectMembers().stream().anyMatch(m -> m.getUserId().equals(userId));
        if (!isOwner && !isMember) {
            throw new IllegalArgumentException("User is not a member of this project");
        }

        String oldAssignee = issue.getAssignedUser() != null ? issue.getAssignedUser().getFullName() : "Unassigned";
        issue.setAssignedUser(user);
        Issue updated = issueRepository.save(issue);
        auditService.log(actorEmail, issue.getProject().getProjectId(), AuditEntityType.ISSUE,
                issueId, AuditAction.ASSIGNED, oldAssignee, updated.getAssignedUser().getFullName());

        if (notificationService != null && !user.getEmail().equalsIgnoreCase(actorEmail)) {
            notificationService.createNotification(
                    user,
                    "Assigned to Issue",
                    "You were assigned to issue: " + updated.getIssueTitle(),
                    "/project/" + project.getProjectId() + "?issue=" + issueId
            );
        }

        return updated;
    }

    @Transactional
    public Issue removeUserFromIssue(Long issueId, String actorEmail) throws Exception {
        Issue issue = getIssueById(issueId);
        projectService.ensureProjectOwner(issue.getProject().getProjectId(), actorEmail);
        String oldAssignee = issue.getAssignedUser() != null ? issue.getAssignedUser().getFullName() : "Unassigned";
        issue.setAssignedUser(null);
        Issue updated = issueRepository.save(issue);
        auditService.log(actorEmail, issue.getProject().getProjectId(), AuditEntityType.ISSUE,
                issueId, AuditAction.ASSIGNED, oldAssignee, "Unassigned");
        return updated;
    }

    @Transactional
    public Issue updateIssueStatus(Long issueId, String status) throws Exception {
        Issue existingIssue = getIssueById(issueId);
        existingIssue.setIssueStatus(IssueStatus.from(status).name());
        return issueRepository.save(existingIssue);
    }

    @Transactional
    public Issue updateIssueStatus(Long issueId, String status, String actorEmail) throws Exception {
        Issue existingIssue = getIssueById(issueId);
        projectService.ensureProjectMemberOrOwner(existingIssue.getProject().getProjectId(), actorEmail);
        String oldStatus = existingIssue.getIssueStatus();
        Issue updated = updateIssueStatus(issueId, status);
        auditService.log(actorEmail, existingIssue.getProject().getProjectId(), AuditEntityType.ISSUE,
                issueId, AuditAction.STATUS_CHANGED, oldStatus, updated.getIssueStatus());

        if (notificationService != null && updated.getAssignedUser() != null && !updated.getAssignedUser().getEmail().equalsIgnoreCase(actorEmail)) {
            notificationService.createNotification(
                    updated.getAssignedUser(),
                    "Issue Status Updated",
                    "Issue \"" + updated.getIssueTitle() + "\" status changed to " + updated.getIssueStatus(),
                    "/project/" + existingIssue.getProject().getProjectId() + "?issue=" + issueId
            );
        }

        return updated;
    }

}
