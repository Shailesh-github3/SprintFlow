package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sprintflow.dto.ProjectStatsDTO;
import com.sprintflow.entity.AuditAction;
import com.sprintflow.entity.AuditEntityType;
import com.sprintflow.entity.Issue;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.repository.IssueRepository;
import com.sprintflow.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceNewFeaturesTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserService userService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    private ProjectService projectService;

    private User owner;
    private User member;
    private Project project;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, userService, subscriptionService,
                issueRepository, auditService, notificationService);

        owner = new User();
        owner.setUserId(1L);
        owner.setEmail("owner@example.com");
        owner.setFullName("Owner User");

        member = new User();
        member.setUserId(2L);
        member.setEmail("member@example.com");
        member.setFullName("Member User");

        project = new Project();
        project.setProjectId(10L);
        project.setProjectName("SprintFlow");
        project.setProjectOwner(owner);
        project.setProjectMembers(new ArrayList<>());
        project.getProjectMembers().add(owner);
        project.getProjectMembers().add(member);
    }

    @Test
    @DisplayName("removeMember successfully removes member, unassigns their tasks, logs audit, and sends notification")
    void removeMember_success() throws Exception {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userService.findByEmail("owner@example.com")).thenReturn(owner);
        when(userService.getUserByUserId(2L)).thenReturn(member);

        Issue assignedIssue = new Issue();
        assignedIssue.setIssueId(55L);
        assignedIssue.setAssignedUser(member);
        when(issueRepository.findByProject_ProjectIdAndAssignedUser_UserId(10L, 2L))
                .thenReturn(List.of(assignedIssue));

        projectService.removeMember(10L, 2L, "owner@example.com");

        assertFalse(project.getProjectMembers().contains(member));
        verify(projectRepository).save(project);
        verify(issueRepository).save(assignedIssue);
        verify(auditService).log(eq("owner@example.com"), eq(10L), eq(AuditEntityType.PROJECT_MEMBER), eq(2L), eq(AuditAction.REMOVED), any(), any());
        verify(notificationService).createNotification(eq(member), eq("Removed from Project"), any(), any());
    }

    @Test
    @DisplayName("removeMember rejects removing the project owner")
    void removeMember_cannotRemoveOwner() throws Exception {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userService.findByEmail("owner@example.com")).thenReturn(owner);
        when(userService.getUserByUserId(1L)).thenReturn(owner);

        assertThrows(IllegalArgumentException.class, () ->
                projectService.removeMember(10L, 1L, "owner@example.com"));
    }

    @Test
    @DisplayName("leaveProject allows non-owner member to leave")
    void leaveProject_success() throws Exception {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userService.findByEmail("member@example.com")).thenReturn(member);
        when(issueRepository.findByProject_ProjectIdAndAssignedUser_UserId(10L, 2L))
                .thenReturn(new ArrayList<>());

        projectService.leaveProject(10L, "member@example.com");

        assertFalse(project.getProjectMembers().contains(member));
        verify(projectRepository).save(project);
        verify(auditService).log(eq("member@example.com"), eq(10L), eq(AuditEntityType.PROJECT_MEMBER), eq(2L), eq(AuditAction.LEFT), any(), any());
    }

    @Test
    @DisplayName("leaveProject rejects project owner attempting to leave")
    void leaveProject_ownerCannotLeave() throws Exception {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userService.findByEmail("owner@example.com")).thenReturn(owner);

        assertThrows(IllegalArgumentException.class, () ->
                projectService.leaveProject(10L, "owner@example.com"));
    }

    @Test
    @DisplayName("getProjectStats returns correctly calculated metrics")
    void getProjectStats_calculatesAccurately() throws Exception {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userService.findByEmail("owner@example.com")).thenReturn(owner);

        when(issueRepository.countTotalIssuesByProject(10L)).thenReturn(10L);
        when(issueRepository.countIssuesByProjectAndStatus(10L, "TODO")).thenReturn(4L);
        when(issueRepository.countIssuesByProjectAndStatus(10L, "IN_PROGRESS")).thenReturn(3L);
        when(issueRepository.countIssuesByProjectAndStatus(10L, "DONE")).thenReturn(3L);
        when(issueRepository.countOverdueIssuesByProject(10L)).thenReturn(2L);

        ProjectStatsDTO stats = projectService.getProjectStats(10L, "owner@example.com");

        assertEquals(10L, stats.totalIssues());
        assertEquals(4L, stats.todoIssues());
        assertEquals(3L, stats.inProgressIssues());
        assertEquals(3L, stats.completedIssues());
        assertEquals(2L, stats.overdueIssues());
        assertEquals(30.0, stats.completionPercentage());
    }
}
