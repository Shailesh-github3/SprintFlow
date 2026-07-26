package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.exception.ResourceNotFoundException;
import com.sprintflow.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceUnitTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserService userService;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ProjectService projectService;

    private User owner;
    private User nonOwner;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setUserId(1L);
        owner.setEmail("owner@example.com");

        nonOwner = new User();
        nonOwner.setUserId(2L);
        nonOwner.setEmail("nonowner@example.com");

        project = new Project();
        project.setProjectId(100L);
        project.setProjectName("Test Project");
        project.setProjectOwner(owner);
        project.setProjectMembers(new ArrayList<>());
        project.getProjectMembers().add(owner);
    }

    @Test
    @DisplayName("ensureProjectOwner throws SecurityException when user is NOT the project owner")
    void ensureProjectOwner_ThrowsException_WhenUserIsNotOwner() throws Exception {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userService.findByEmail("nonowner@example.com")).thenReturn(nonOwner);

        SecurityException thrown = assertThrows(
                SecurityException.class,
                () -> projectService.ensureProjectOwner(100L, "nonowner@example.com"),
                "Expected SecurityException when a non-owner tries to assert ownership"
        );
        assertEquals("User is not authorized for this project", thrown.getMessage());
    }

    @Test
    @DisplayName("ensureProjectOwner succeeds when user IS the project owner")
    void ensureProjectOwner_Succeeds_WhenUserIsOwner() throws Exception {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userService.findByEmail("owner@example.com")).thenReturn(owner);

        assertDoesNotThrow(() -> projectService.ensureProjectOwner(100L, "owner@example.com"));
    }

    @Test
    @DisplayName("ensureProjectOwner throws ResourceNotFoundException when project does not exist")
    void ensureProjectOwner_ThrowsResourceNotFound_WhenProjectMissing() throws Exception {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> projectService.ensureProjectOwner(999L, "anyone@example.com"),
                "Expected ResourceNotFoundException for a non-existent project"
        );
    }

    @Test
    @DisplayName("ensureProjectMemberOrOwner throws SecurityException for non-member non-owner")
    void ensureProjectMemberOrOwner_ThrowsException_WhenUserIsNeither() throws Exception {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userService.findByEmail("nonowner@example.com")).thenReturn(nonOwner);

        assertThrows(
                SecurityException.class,
                () -> projectService.ensureProjectMemberOrOwner(100L, "nonowner@example.com"),
                "Expected SecurityException when user is neither owner nor member"
        );
    }

    @Test
    @DisplayName("createProject returns saved project with owner in members list")
    void createProject_SetsOwnerAndChat() {
        Project inputProject = new Project();
        inputProject.setProjectName("New Project");
        inputProject.setProjectDescription("A test project");

        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setProjectId(42L);
            return saved;
        });

        Project result = projectService.createProject(inputProject, owner);

        assertNotNull(result);
        assertEquals(42L, result.getProjectId());
        assertEquals(owner, result.getProjectOwner());
        assertNotNull(result.getChat(), "Chat should be created for the project");
        assertNotNull(result.getProjectMembers(), "Project members should not be null");
        assertEquals(true, result.getProjectMembers().contains(owner),
                "Owner should be auto-added to project members");
    }
}
