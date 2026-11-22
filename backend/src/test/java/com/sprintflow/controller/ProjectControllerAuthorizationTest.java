package com.sprintflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.config.JwtService;
import com.sprintflow.config.JwtTokenValidator;
import com.sprintflow.dto.AuditLogDTO;
import com.sprintflow.dto.ProjectStatsDTO;
import com.sprintflow.entity.AuditLog;
import com.sprintflow.entity.Invitation;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.request.InviteRequest;
import com.sprintflow.request.ProjectRequest;
import com.sprintflow.service.AuditService;
import com.sprintflow.service.InvitationService;
import com.sprintflow.service.ProjectService;
import com.sprintflow.service.UserService;
import com.sprintflow.mapper.ProjectMapper;
import com.sprintflow.mapper.ChatMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
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
    private AuditService auditService;

    @MockitoBean
    private ProjectMapper projectMapper;

    @MockitoBean
    private ChatMapper chatMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void createProject_shouldReturn201() throws Exception {
        ProjectRequest request = new ProjectRequest();
        request.setProjectName("New Project");
        request.setProjectDescription("Description");
        request.setCategory("Tech");

        User owner = new User();
        owner.setUserId(1L);
        owner.setEmail("owner@example.com");

        Project created = new Project();
        created.setProjectId(1L);
        created.setProjectName("New Project");

        when(userService.findByEmail("owner@example.com")).thenReturn(owner);
        when(projectService.createProject(any(Project.class), eq(owner))).thenReturn(created);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "bob@example.com", roles = "USER")
    void getProjectById_shouldReturn403_whenNotMember() throws Exception {
        doThrow(new SecurityException("User is not authorized for this project"))
                .when(projectService)
                .ensureProjectMemberOrOwner(anyLong(), anyString());

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void inviteUserToProject_shouldReturn200_whenValid() throws Exception {
        InviteRequest request = new InviteRequest();
        request.setProjectId(1L);
        request.setUserEmail("invitee@example.com");

        User invitee = new User();
        invitee.setUserId(2L);
        invitee.setEmail("invitee@example.com");

        when(userService.getUserByEmail("invitee@example.com")).thenReturn(invitee);

        mockMvc.perform(post("/api/projects/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(invitationService).sendInvitation("invitee@example.com", 1L, "owner@example.com", 2L);
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void acceptInvitation_shouldReturn200() throws Exception {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("alice@example.com");

        when(userService.findByEmail("alice@example.com")).thenReturn(user);

        mockMvc.perform(get("/api/projects/invite")
                        .param("token", "valid-token"))
                .andExpect(status().isOk());

        verify(invitationService).acceptInvitationForUser("valid-token", "alice@example.com", 1L);
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void getProjectActivity_shouldReturn200_withSummaries_whenMember() throws Exception {
        AuditLog entry = new AuditLog();
        entry.setAuditId(1L);
        AuditLogDTO dto = new AuditLogDTO();
        dto.setAuditId(1L);
        dto.setActorName("John");
        dto.setSummary("John created issue #42");

        when(auditService.getActivityForProject(anyLong(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(entry)));
        when(auditService.toDto(entry)).thenReturn(dto);

        mockMvc.perform(get("/api/projects/9/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].summary").value("John created issue #42"));
    }

    @Test
    @WithMockUser(username = "bob@example.com", roles = "USER")
    void getProjectActivity_shouldReturn403_whenNotMember() throws Exception {
        doThrow(new SecurityException("User is not authorized for this project"))
                .when(projectService)
                .ensureProjectMemberOrOwner(anyLong(), anyString());

        mockMvc.perform(get("/api/projects/9/activity"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    @DisplayName("removeMember deletes member and returns 200")
    void removeMember_shouldReturn200_whenInvokedByOwner() throws Exception {
        mockMvc.perform(delete("/api/projects/5/members/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Member removed successfully."));

        verify(projectService).removeMember(5L, 12L, "owner@example.com");
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    @DisplayName("leaveProject removes caller from project and returns 200")
    void leaveProject_shouldReturn200_whenMemberLeaves() throws Exception {
        mockMvc.perform(post("/api/projects/5/leave"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("You have left the project."));

        verify(projectService).leaveProject(5L, "member@example.com");
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    @DisplayName("getProjectStats returns project stats")
    void getProjectStats_shouldReturn200_withStats() throws Exception {
        ProjectStatsDTO stats = new ProjectStatsDTO(5L, 10L, 3L, 2L, 5L, 1L, 50.0);
        when(projectService.getProjectStats(5L, "alice@example.com")).thenReturn(stats);

        mockMvc.perform(get("/api/projects/5/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(5))
                .andExpect(jsonPath("$.totalIssues").value(10))
                .andExpect(jsonPath("$.completedIssues").value(5))
                .andExpect(jsonPath("$.completionPercentage").value(50.0));
    }

    @Test
    @WithMockUser(username = "invitee@example.com", roles = "USER")
    @DisplayName("rejectInvitation deletes token and returns 200")
    void rejectInvitation_shouldReturn200_whenValidToken() throws Exception {
        mockMvc.perform(post("/api/projects/invite/reject")
                        .param("token", "reject-me-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation declined successfully."));

        verify(invitationService).rejectInvitationForUser("reject-me-token", "invitee@example.com");
    }
}
