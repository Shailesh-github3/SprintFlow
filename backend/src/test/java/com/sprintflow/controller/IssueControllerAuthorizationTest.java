package com.sprintflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.config.JwtService;
import com.sprintflow.config.JwtTokenValidator;
import com.sprintflow.dto.IssueDTO;
import com.sprintflow.entity.Issue;
import com.sprintflow.request.IssueRequest;
import com.sprintflow.request.UpdateIssueRequest;
import com.sprintflow.service.IssueService;
import com.sprintflow.service.ProjectService;
import com.sprintflow.mapper.IssueMapper;

import org.junit.jupiter.api.DisplayName;
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
        request.setStatus("TODO");
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

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void createIssue_shouldReturn400_whenTitleIsBlank() throws Exception {
        IssueRequest request = new IssueRequest();
        request.setTitle("");
        request.setDescription("Fix the bug");
        request.setStatus("TODO");
        request.setProjectId(1L);
        request.setPriority("HIGH");
        request.setDueDate(LocalDate.now().plusDays(7));

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void createIssue_shouldReturn400_whenStatusIsInvalid() throws Exception {
        IssueRequest request = new IssueRequest();
        request.setTitle("Bug Fix");
        request.setDescription("Fix the bug");
        request.setStatus("OPEN");
        request.setProjectId(1L);
        request.setPriority("HIGH");
        request.setDueDate(LocalDate.now().plusDays(7));

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    @DisplayName("createIssue returns 400 when due date is in the past")
    void createIssue_shouldReturn400_whenDueDateIsInThePast() throws Exception {
        IssueRequest request = new IssueRequest();
        request.setTitle("Bug Fix");
        request.setDescription("Fix the bug");
        request.setStatus("TODO");
        request.setProjectId(1L);
        request.setPriority("HIGH");
        request.setDueDate(LocalDate.now().minusDays(1)); // past date

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    @DisplayName("updateIssue returns 200 when payload is valid")
    void updateIssue_shouldReturn200_whenValid() throws Exception {
        UpdateIssueRequest updateRequest = new UpdateIssueRequest("Updated Title", "Updated Description", "HIGH", LocalDate.now().plusDays(5), "IN_PROGRESS");
        Issue updatedIssue = new Issue();
        updatedIssue.setIssueId(10L);
        updatedIssue.setIssueTitle("Updated Title");

        IssueDTO dto = new IssueDTO();
        dto.setIssueId(10L);
        dto.setIssueTitle("Updated Title");

        when(issueService.updateIssue(eq(10L), any(UpdateIssueRequest.class), eq("alice@example.com")))
                .thenReturn(updatedIssue);
        when(issueMapper.toDto(updatedIssue)).thenReturn(dto);

        mockMvc.perform(put("/api/issues/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueTitle").value("Updated Title"));
    }

}
