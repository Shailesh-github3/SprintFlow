package com.sprintflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.config.JwtService;
import com.sprintflow.config.JwtTokenValidator;
import com.sprintflow.entity.Issue;
import com.sprintflow.request.IssueRequest;
import com.sprintflow.service.IssueService;
import com.sprintflow.service.ProjectService;
import com.sprintflow.mapper.IssueMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IssueController.class)
@AutoConfigureMockMvc(addFilters = false)
class IssueControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IssueService issueService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @MockitoBean
    private IssueMapper issueMapper;

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void createIssue_shouldReturn201_whenAuthorized() throws Exception {

        IssueRequest request = new IssueRequest();
        request.setTitle("Bug Fix");
        request.setDescription("Fix the bug");
        request.setStatus("OPEN");
        request.setProjectId(1L);
        request.setPriority("HIGH");
        request.setDueDate(LocalDate.now().plusDays(7));

        Issue issue = new Issue();
        issue.setIssueId(1L);
        issue.setIssueTitle("Bug Fix");

        when(issueService.createIssue(any(IssueRequest.class), anyString()))
                .thenReturn(issue);

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void getIssuesByProjectId_shouldReturn403_whenAuthorizationFails() throws Exception {

        doThrow(new SecurityException("User is not authorized for this project"))
                .when(projectService)
                .ensureProjectMemberOrOwner(anyLong(), anyString());

        mockMvc.perform(get("/api/issues/project/1"))
                .andExpect(status().isForbidden());
    }

}