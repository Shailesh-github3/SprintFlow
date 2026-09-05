package com.sprintflow.controller;

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
import com.sprintflow.entity.Issue;
import com.sprintflow.entity.Project;
import com.sprintflow.mapper.CommentMapper;
import com.sprintflow.service.CommentService;
import com.sprintflow.service.IssueService;
import com.sprintflow.service.ProjectService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private IssueService issueService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private CommentMapper commentMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @Test
    @DisplayName("rejects blank comment content")
    void createComment_blankContent_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.sprintflow.request.CreateCommentRequest(1L, ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "bob@example.com", roles = "USER")
    @DisplayName("forbids listing comments on an issue in a project the user is not a member of")
    void getCommentsByIssueId_nonMember_returnsForbidden() throws Exception {
        Project project = new Project();
        project.setProjectId(9L);
        Issue issue = new Issue();
        issue.setIssueId(1L);
        issue.setProject(project);

        when(issueService.getIssueById(1L)).thenReturn(issue);
        doThrow(new SecurityException("User is not authorized for this project"))
                .when(projectService).ensureProjectMemberOrOwner(anyLong(), anyString());

        mockMvc.perform(get("/api/comments/issue/1"))
                .andExpect(status().isForbidden());
    }
}