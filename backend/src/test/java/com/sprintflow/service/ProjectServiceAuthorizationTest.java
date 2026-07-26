package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceAuthorizationTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserService userService;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void ensureProjectOwner_shouldRejectNonOwner() throws Exception {
        User owner = new User();
        owner.setUserId(1L);
        owner.setEmail("owner@example.com");

        Project project = new Project();
        project.setProjectId(99L);
        project.setProjectOwner(owner);

        User actor = new User();
        actor.setUserId(2L);
        actor.setEmail("other@example.com");

        when(projectRepository.findById(99L)).thenReturn(Optional.of(project));
        when(userService.findByEmail("other@example.com")).thenReturn(actor);

        assertThrows(SecurityException.class, () -> projectService.ensureProjectOwner(99L, "other@example.com"));
    }
}
