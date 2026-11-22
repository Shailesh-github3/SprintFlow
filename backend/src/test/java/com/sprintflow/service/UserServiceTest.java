package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sprintflow.dto.DashboardStatsDTO;
import com.sprintflow.entity.User;
import com.sprintflow.repository.IssueRepository;
import com.sprintflow.repository.ProjectRepository;
import com.sprintflow.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private IssueRepository issueRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, projectRepository, issueRepository);
    }

    @Test
    @DisplayName("getDashboardStats aggregates project and issue counts accurately")
    void getDashboardStats_success() {
        User user = new User();
        user.setUserId(5L);
        user.setEmail("user@example.com");

        when(projectRepository.countProjectsForUser(user)).thenReturn(3L);
        when(issueRepository.countTotalIssuesForUser(user)).thenReturn(20L);
        when(issueRepository.countIssuesForUserByStatus(user, "TODO")).thenReturn(5L);
        when(issueRepository.countIssuesForUserByStatus(user, "IN_PROGRESS")).thenReturn(5L);
        when(issueRepository.countIssuesForUserByStatus(user, "DONE")).thenReturn(10L);
        when(issueRepository.countOverdueIssuesForUser(user)).thenReturn(2L);

        DashboardStatsDTO stats = userService.getDashboardStats(user);

        assertEquals(3L, stats.totalProjects());
        assertEquals(20L, stats.totalIssues());
        assertEquals(5L, stats.todoIssues());
        assertEquals(5L, stats.inProgressIssues());
        assertEquals(10L, stats.completedIssues());
        assertEquals(2L, stats.overdueIssues());
        assertEquals(50.0, stats.completionPercentage());
    }
}
