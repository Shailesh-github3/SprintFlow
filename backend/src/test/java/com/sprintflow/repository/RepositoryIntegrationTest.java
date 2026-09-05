package com.sprintflow.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.sprintflow.entity.Issue;
import com.sprintflow.entity.PlanType;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("sprintflow_repository_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("enforces unique email constraints at the database layer")
    void uniqueEmailConstraint_shouldRejectDuplicateUsers() {
        // Given
        User first = buildUser("unique@example.com");
        userRepository.saveAndFlush(first);

        User duplicate = buildUser("unique@example.com");

        // When / Then
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(duplicate);
            userRepository.flush();
        });
    }

    @Test
    @DisplayName("uses EntityGraph to initialize project owner and issue collection")
    void findProjectsForMemberWithFilters_shouldInitializeGraph() {
        // Given
        User owner = userRepository.saveAndFlush(buildUser("owner@example.com"));
        User member = userRepository.saveAndFlush(buildUser("member@example.com"));

        Project project = new Project();
        project.setProjectName("Graph Project");
        project.setProjectDescription("Project for EntityGraph assertions");
        project.setCategory("Software");
        project.setProjectOwner(owner);
        project.setProjectMembers(new java.util.ArrayList<>(List.of(owner, member)));
        project = projectRepository.saveAndFlush(project);

        Issue issue = new Issue();
        issue.setIssueTitle("Graph issue");
        issue.setIssueDescription("Issue to keep the association populated");
        issue.setIssueStatus("TODO");
        issue.setPriority("HIGH");
        issue.setProject(project);
        issueRepository.saveAndFlush(issue);

        entityManager.clear();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        // When
        Page<Project> page = projectRepository.findProjectsForMemberWithFilters(member, null, null, PageRequest.of(0, 10));
        Project fetched = page.getContent().get(0);

        // Then
        assertTrue(Hibernate.isInitialized(fetched.getProjectOwner()));
        assertTrue(Hibernate.isInitialized(fetched.getIssues()));
        assertEquals(1, fetched.getIssues().size());
    }

    @Test
    @DisplayName("rejects direct project deletion when child issues still reference it")
    void deleteProject_withReferencingIssue_shouldFailWithForeignKeyViolation() {
        // Given
        User owner = userRepository.saveAndFlush(buildUser("fk-owner@example.com"));
        Project project = new Project();
        project.setProjectName("Delete Me");
        project.setProjectDescription("Project with children");
        project.setCategory("Software");
        project.setProjectOwner(owner);
        project.setProjectMembers(new java.util.ArrayList<>(List.of(owner)));
        project = projectRepository.saveAndFlush(project);

        Issue issue = new Issue();
        issue.setIssueTitle("Blocking issue");
        issue.setIssueDescription("Keeps the FK active");
        issue.setIssueStatus("TODO");
        issue.setPriority("HIGH");
        issue.setProject(project);
        issueRepository.saveAndFlush(issue);

        // When / Then
        final Long projectId = project.getProjectId();
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update("DELETE FROM project WHERE project_id = ?", projectId));
    }

    private User buildUser(String email) {
        User user = new User();
        user.setFullName(email.split("@")[0]);
        user.setEmail(email);
        user.setPassword("encoded-password-123");
        user.setRoles(List.of("USER"));
        user.setProjectSize(1);
        return user;
    }
}