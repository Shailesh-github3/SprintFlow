package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
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
import com.sprintflow.request.UpdateIssueRequest;

@ExtendWith(MockitoExtension.class)
class IssueServiceNewFeaturesTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    private IssueService issueService;

    private User owner;
    private User creator;
    private User otherUser;
    private Project project;
    private Issue issue;

    @BeforeEach
    void setUp() {
        issueService = new IssueService(issueRepository, projectService, userService, auditService, notificationService);

        owner = new User();
        owner.setUserId(1L);
        owner.setEmail("owner@example.com");
        owner.setFullName("Owner User");

        creator = new User();
        creator.setUserId(2L);
        creator.setEmail("creator@example.com");
        creator.setFullName("Creator User");

        otherUser = new User();
        otherUser.setUserId(3L);
        otherUser.setEmail("other@example.com");
        otherUser.setFullName("Other User");

        project = new Project();
        project.setProjectId(10L);
        project.setProjectName("SprintFlow App");
        project.setProjectOwner(owner);
        project.setProjectMembers(new ArrayList<>());
        project.getProjectMembers().add(owner);
        project.getProjectMembers().add(creator);
        project.getProjectMembers().add(otherUser);

        issue = new Issue();
        issue.setIssueId(100L);
        issue.setIssueTitle("Initial Title");
        issue.setIssueDescription("Initial Description");
        issue.setPriority("LOW");
        issue.setIssueStatus("TODO");
        issue.setDueDate(LocalDate.now().plusDays(5));
        issue.setProject(project);
        issue.setCreatedBy(creator);
    }

    @Test
    @DisplayName("updateIssue updates fields and writes an audit log")
    void updateIssue_success() throws Exception {
        when(issueRepository.findById(100L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenAnswer(i -> i.getArgument(0));

        UpdateIssueRequest req = new UpdateIssueRequest("Updated Title", "Updated Description", "HIGH", LocalDate.now().plusDays(10), "IN_PROGRESS");
        Issue updated = issueService.updateIssue(100L, req, "creator@example.com");

        assertEquals("Updated Title", updated.getIssueTitle());
        assertEquals("Updated Description", updated.getIssueDescription());
        assertEquals("HIGH", updated.getPriority());
        assertEquals("IN_PROGRESS", updated.getIssueStatus());
        verify(auditService).log("creator@example.com", 10L, AuditEntityType.ISSUE, 100L, AuditAction.UPDATED, null, null);
    }

    @Test
    @DisplayName("deleteIssue allows issue creator to delete")
    void deleteIssue_creatorAllowed() throws Exception {
        when(issueRepository.findById(100L)).thenReturn(Optional.of(issue));

        assertDoesNotThrow(() -> issueService.deleteIssue(100L, "creator@example.com"));
        verify(issueRepository).delete(issue);
        verify(auditService).log(eq("creator@example.com"), eq(10L), eq(AuditEntityType.ISSUE), eq(100L), eq(AuditAction.DELETED), any(), any());
    }

    @Test
    @DisplayName("deleteIssue allows project owner to delete")
    void deleteIssue_ownerAllowed() throws Exception {
        when(issueRepository.findById(100L)).thenReturn(Optional.of(issue));

        assertDoesNotThrow(() -> issueService.deleteIssue(100L, "owner@example.com"));
        verify(issueRepository).delete(issue);
        verify(auditService).log(eq("owner@example.com"), eq(10L), eq(AuditEntityType.ISSUE), eq(100L), eq(AuditAction.DELETED), any(), any());
    }

    @Test
    @DisplayName("deleteIssue rejects non-owner non-creator with SecurityException")
    void deleteIssue_otherUserRejected() {
        when(issueRepository.findById(100L)).thenReturn(Optional.of(issue));

        assertThrows(SecurityException.class, () -> issueService.deleteIssue(100L, "other@example.com"));
    }

    @Test
    @DisplayName("assigning issue dispatches in-app notification to assignee")
    void addUserToIssue_sendsNotification() throws Exception {
        when(issueRepository.findById(100L)).thenReturn(Optional.of(issue));
        when(userService.getUserByUserId(3L)).thenReturn(otherUser);
        when(issueRepository.save(any(Issue.class))).thenAnswer(i -> i.getArgument(0));

        issueService.addUserToIssue(100L, 3L, "owner@example.com");

        verify(notificationService).createNotification(
                eq(otherUser),
                eq("Assigned to Issue"),
                any(),
                any()
        );
    }
}
