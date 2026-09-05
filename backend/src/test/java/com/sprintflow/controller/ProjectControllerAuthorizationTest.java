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
import com.sprintflow.dto.AuditLogDTO;
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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

        @Test
        @WithMockUser(username = "alice@example.com", roles = "USER")
        void inviteUserToProject_shouldReturn400_whenEmailIsInvalid() throws Exception {
                InviteRequest request = new InviteRequest();
                request.setProjectId(1L);
                request.setUserEmail("not-an-email");

                mockMvc.perform(post("/api/projects/invite")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "alice@example.com", roles = "USER")
        void acceptInvitation_shouldReturn200_andAddTeamMember_whenTokenMatchesCurrentUser() throws Exception {
                Invitation invitation = new Invitation();
                invitation.setProjectId(42L);
                invitation.setUserEmail("alice@example.com");
                invitation.setToken("invitation-token");

                User currentUser = new User();
                currentUser.setUserId(7L);
                currentUser.setEmail("alice@example.com");

                when(invitationService.acceptInvitationForUser("invitation-token", "alice@example.com", 7L)).thenReturn(invitation);
                when(userService.findByEmail("alice@example.com")).thenReturn(currentUser);

                mockMvc.perform(get("/api/projects/invite").param("token", "invitation-token"))
                                .andExpect(status().isOk());

                verify(invitationService).acceptInvitationForUser("invitation-token", "alice@example.com", 7L);
        }

        @Test
        @WithMockUser(username = "bob@example.com", roles = "USER")
        void acceptInvitation_shouldReturn403_whenTokenBelongsToAnotherUser() throws Exception {
                User currentUser = new User();
                currentUser.setUserId(8L);
                currentUser.setEmail("bob@example.com");

                when(userService.findByEmail("bob@example.com")).thenReturn(currentUser);
                doThrow(new SecurityException("This invitation was not sent to you"))
                        .when(invitationService).acceptInvitationForUser("invitation-token", "bob@example.com", 8L);

                mockMvc.perform(get("/api/projects/invite").param("token", "invitation-token"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "alice@example.com", roles = "USER")
        void acceptInvitation_shouldReturn400_whenTokenIsInvalid() throws Exception {
                User currentUser = new User();
                currentUser.setUserId(7L);
                currentUser.setEmail("alice@example.com");

                when(userService.findByEmail("alice@example.com")).thenReturn(currentUser);
                doThrow(new IllegalArgumentException("Invalid invitation token"))
                        .when(invitationService).acceptInvitationForUser("invitation-token", "alice@example.com", 7L);

                mockMvc.perform(get("/api/projects/invite").param("token", "invitation-token"))
                                .andExpect(status().isBadRequest());
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
}