package com.sprintflow.service;

import java.util.Optional;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sprintflow.entity.Issue;
import com.sprintflow.repository.IssueRepository;
import com.sprintflow.request.IssueRequest;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectService projectService;
    private final UserService userService;

    public IssueService(IssueRepository issueRepository, ProjectService projectService, UserService userService) {
        this.issueRepository = issueRepository;
        this.projectService = projectService;
        this.userService = userService;
    }

    @Transactional
    public Issue createIssue(IssueRequest issue) throws Exception {
        Project project = projectService.getProjectById(issue.getProjectId());
        Issue newIssue = new Issue();
        newIssue.setIssueTitle(issue.getTitle());
        newIssue.setIssueDescription(issue.getDescription());
        newIssue.setIssueStatus(issue.getStatus());
        newIssue.setPriority(issue.getPriority());
        newIssue.setDueDate(issue.getDueDate());
        newIssue.setProject(project);
        return issueRepository.save(newIssue);
    }

    @Transactional
    public Issue createIssue(IssueRequest issue, String actorEmail) throws Exception {
        projectService.ensureProjectMemberOrOwner(issue.getProjectId(), actorEmail);
        return createIssue(issue);
    }

    @Transactional(readOnly = true)
    public Issue getIssueById(Long issueId) throws Exception {
        Long resolvedIssueId = Objects.requireNonNull(issueId, "issueId must not be null");
        Optional<Issue> issue = issueRepository.findById(resolvedIssueId);
        if (!issue.isPresent()) {
            throw new com.sprintflow.exception.ResourceNotFoundException("Issue not found with id: " + issueId);
        }
        return issue.get();
    }

    @Transactional(readOnly = true)
    public Page<Issue> getIssuesByProjectId(Long projectId, Pageable pageable) {
        return issueRepository.findByProject_ProjectId(projectId, pageable);
    }

    @Transactional
    public void deleteIssue(Long issueId) throws Exception {
        Long resolvedIssueId = Objects.requireNonNull(issueId, "issueId must not be null");
        Optional<Issue> issue = issueRepository.findById(resolvedIssueId);
        if (!issue.isPresent()) {
            throw new com.sprintflow.exception.ResourceNotFoundException("Issue not found with id: " + resolvedIssueId);
        }
        issueRepository.deleteById(resolvedIssueId);
    }

    @Transactional
    public void deleteIssue(Long issueId, String actorEmail) throws Exception {
        Issue issue = getIssueById(issueId);
        projectService.ensureProjectMemberOrOwner(issue.getProject().getProjectId(), actorEmail);
        deleteIssue(issueId);
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
        return addUserToIssue(issueId, userId);
    }

    @Transactional
    public Issue updateIssue(Long issueId, IssueRequest issueRequest) throws Exception {
        Issue existingIssue = getIssueById(issueId);
        existingIssue.setIssueTitle(issueRequest.getTitle());
        existingIssue.setIssueDescription(issueRequest.getDescription());
        existingIssue.setIssueStatus(issueRequest.getStatus());
        existingIssue.setPriority(issueRequest.getPriority());
        existingIssue.setDueDate(issueRequest.getDueDate());
        return issueRepository.save(existingIssue);
    }

    @Transactional
    public Issue updateIssue(Long issueId, IssueRequest issueRequest, String actorEmail) throws Exception {
        Issue existingIssue = getIssueById(issueId);
        projectService.ensureProjectMemberOrOwner(existingIssue.getProject().getProjectId(), actorEmail);
        return updateIssue(issueId, issueRequest);
    }

    @Transactional
    public Issue updateIssueStatus(Long issueId, String status) throws Exception {
        Issue existingIssue = getIssueById(issueId);
        existingIssue.setIssueStatus(status);
        return issueRepository.save(existingIssue);
    }

    @Transactional
    public Issue updateIssueStatus(Long issueId, String status, String actorEmail) throws Exception {
        Issue existingIssue = getIssueById(issueId);
        projectService.ensureProjectMemberOrOwner(existingIssue.getProject().getProjectId(), actorEmail);
        return updateIssueStatus(issueId, status);
    }

}
