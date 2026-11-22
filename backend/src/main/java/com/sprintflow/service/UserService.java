package com.sprintflow.service;

import com.sprintflow.dto.DashboardStatsDTO;
import com.sprintflow.repository.IssueRepository;
import com.sprintflow.repository.ProjectRepository;
import com.sprintflow.repository.UserRepository;
import com.sprintflow.entity.User;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;

    public UserService(UserRepository userRepository) {
        this(userRepository, null, null);
    }

    @Autowired
    public UserService(UserRepository userRepository,
                       @Autowired(required = false) ProjectRepository projectRepository,
                       @Autowired(required = false) IssueRepository issueRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new BadCredentialsException("User not found");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User getUserByUserId(Long userId) {
        Long resolvedUserId = Objects.requireNonNull(userId, "userId must not be null");
        return userRepository.findById(resolvedUserId)
                .orElseThrow(() -> new RuntimeException("User not found with userId: " + userId));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail);
    }

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(User user) {
        long totalProjects = projectRepository != null ? projectRepository.countProjectsForUser(user) : 0;
        long totalIssues = issueRepository != null ? issueRepository.countTotalIssuesForUser(user) : 0;
        long todoIssues = issueRepository != null ? issueRepository.countIssuesForUserByStatus(user, "TODO") : 0;
        long inProgressIssues = issueRepository != null ? issueRepository.countIssuesForUserByStatus(user, "IN_PROGRESS") : 0;
        long doneIssues = issueRepository != null ? issueRepository.countIssuesForUserByStatus(user, "DONE") : 0;
        long overdueIssues = issueRepository != null ? issueRepository.countOverdueIssuesForUser(user) : 0;
        double completionRate = totalIssues > 0 ? ((double) doneIssues / totalIssues) * 100.0 : 0.0;

        return new DashboardStatsDTO(
                totalProjects,
                totalIssues,
                todoIssues,
                inProgressIssues,
                doneIssues,
                overdueIssues,
                Math.round(completionRate * 10.0) / 10.0
        );
    }

}
