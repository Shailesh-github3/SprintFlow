package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sprintflow.entity.AuditAction;
import com.sprintflow.entity.AuditEntityType;
import com.sprintflow.entity.Issue;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.repository.IssueRepository;
import com.sprintflow.request.IssueRequest;

@ExtendWith(MockitoExtension.class)
class IssueServiceAuditTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    private IssueService issueService;

    @BeforeEach
    void setUp() {
        issueService = new IssueService(issueRepository, projectService, userService, auditService);
    }

    @Test
    @DisplayName("creating an issue records a CREATED entry")
    void createIssue_recordsCreated() throws Exception {
        Project project = project(9L);
        when(projectService.getProjectById(9L)).thenReturn(project);
        when(issueRepository.save(any(Issue.class))).thenAnswer(i -> {
            Issue saved = i.getArgument(0);
            saved.setIssueId(42L);
            return saved;
        });

        issueService.createIssue(request(), "john@example.com");

        verify(auditService).log("john@example.com", 9L, AuditEntityType.ISSUE, 42L,
                AuditAction.CREATED, null, null);
    }

    @Test
    @DisplayName("changing status records old and new status")
    void updateIssueStatus_recordsOldAndNew() throws Exception {
        Issue issue = issue(42L, "TODO", null);
        when(issueRepository.findById(42L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenAnswer(i -> i.getArgument(0));

        issueService.updateIssueStatus(42L, "IN_PROGRESS", "priya@example.com");

        verify(auditService).log("priya@example.com", 9L, AuditEntityType.ISSUE, 42L,
                AuditAction.STATUS_CHANGED, "TODO", "IN_PROGRESS");
    }

    @Test
    @DisplayName("assigning records previous and new assignee names")
    void addUserToIssue_recordsAssigneeChange() throws Exception {
        Issue issue = issue(42L, "TODO", user(1L, "Alice"));
        when(issueRepository.findById(42L)).thenReturn(Optional.of(issue));
        when(userService.getUserByUserId(7L)).thenReturn(user(7L, "Bob"));
        when(issueRepository.save(any(Issue.class))).thenAnswer(i -> i.getArgument(0));

        issueService.addUserToIssue(42L, 7L, "john@example.com");

        verify(auditService).log("john@example.com", 9L, AuditEntityType.ISSUE, 42L,
                AuditAction.ASSIGNED, "Alice", "Bob");
    }

    @Test
    @DisplayName("assigning an unassigned issue records Unassigned as the old value")
    void addUserToIssue_unassignedBefore_recordsUnassigned() throws Exception {
        Issue issue = issue(42L, "TODO", null);
        when(issueRepository.findById(42L)).thenReturn(Optional.of(issue));
        when(userService.getUserByUserId(7L)).thenReturn(user(7L, "Bob"));
        when(issueRepository.save(any(Issue.class))).thenAnswer(i -> i.getArgument(0));

        issueService.addUserToIssue(42L, 7L, "john@example.com");

        verify(auditService).log("john@example.com", 9L, AuditEntityType.ISSUE, 42L,
                AuditAction.ASSIGNED, "Unassigned", "Bob");
    }

    private IssueRequest request() {
        IssueRequest request = new IssueRequest();
        request.setTitle("Login bug");
        request.setDescription("Cannot log in");
        request.setStatus("TODO");
        request.setPriority("MEDIUM");
        request.setDueDate(LocalDate.now().plusDays(3));
        request.setProjectId(9L);
        return request;
    }

    private Issue issue(Long id, String status, User assignee) {
        Issue issue = new Issue();
        issue.setIssueId(id);
        issue.setIssueTitle("Login bug");
        issue.setIssueStatus(status);
        issue.setPriority("MEDIUM");
        issue.setProject(project(9L));
        issue.setAssignedUser(assignee);
        return issue;
    }

    @Test
    @DisplayName("assigning a user who is not a project member throws IllegalArgumentException")
    void addUserToIssue_userNotProjectMember_throwsException() throws Exception {
        Issue issue = issue(42L, "TODO", null);
        when(issueRepository.findById(42L)).thenReturn(Optional.of(issue));
        when(userService.getUserByUserId(999L)).thenReturn(user(999L, "Stranger"));

        assertThrows(IllegalArgumentException.class, () ->
                issueService.addUserToIssue(42L, 999L, "john@example.com"));
    }

    @Test
    @DisplayName("removing assignee records unassigned in audit log")
    void removeUserFromIssue_recordsUnassigned() throws Exception {
        Issue issue = issue(42L, "TODO", user(7L, "Bob"));
        when(issueRepository.findById(42L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenAnswer(i -> i.getArgument(0));

        issueService.removeUserFromIssue(42L, "john@example.com");

        verify(auditService).log("john@example.com", 9L, AuditEntityType.ISSUE, 42L,
                AuditAction.ASSIGNED, "Bob", "Unassigned");
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setProjectId(id);
        project.setProjectName("P");
        project.setProjectOwner(user(99L, "Owner"));
        project.setProjectMembers(new java.util.ArrayList<>(java.util.List.of(user(1L, "Alice"), user(7L, "Bob"))));
        return project;
    }

    private User user(Long id, String fullName) {
        User user = new User();
        user.setUserId(id);
        user.setFullName(fullName);
        user.setEmail(fullName.toLowerCase() + "@example.com");
        return user;
    }
}
