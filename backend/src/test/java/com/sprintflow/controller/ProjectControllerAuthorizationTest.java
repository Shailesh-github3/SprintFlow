package com.sprintflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.config.JwtService;
import com.sprintflow.config.JwtTokenValidator;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.request.ProjectRequest;
import com.sprintflow.service.InvitationService;
import com.sprintflow.service.ProjectService;
import com.sprintflow.service.UserService;
import com.sprintflow.mapper.ProjectMapper;
import com.sprintflow.mapper.ChatMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @MockitoBean
    private ProjectMapper projectMapper;

    @MockitoBean
    private ChatMapper chatMapper;

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void createProject_shouldReturn201_whenAuthenticated() throws Exception {

        ProjectRequest request = new ProjectRequest();
        request.setProjectName("Test Project");
        request.setProjectDescription("Description");
        request.setCategory("Software");

        User user = new User();
        user.setUserId(1L);
        user.setEmail("alice@example.com");

        Project project = new Project();
        project.setProjectId(1L);
        project.setProjectName("Test Project");
        project.setProjectOwner(user);

        when(userService.findByEmail("alice@example.com"))
                .thenReturn(user);

        when(projectService.createProject(any(Project.class), any(User.class)))
                .thenReturn(project);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void getProjectById_shouldReturn403_whenAuthorizationFails() throws Exception {

        User user = new User();
        user.setUserId(2L);
        user.setEmail("bob@example.com");

        Project project = new Project();
        project.setProjectId(1L);
        project.setProjectOwner(user);

        when(projectService.getProjectById(1L))
                .thenReturn(project);

        doThrow(new SecurityException("User is not authorized"))
                .when(projectService)
                .ensureProjectMemberOrOwner(anyLong(), anyString());

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isForbidden());
    }
}