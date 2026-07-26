package com.sprintflow.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.config.JwtService;
import com.sprintflow.config.JwtTokenValidator;
import com.sprintflow.entity.Chat;
import com.sprintflow.entity.Message;
import com.sprintflow.entity.Project;
import com.sprintflow.request.CreateMessageRequest;
import com.sprintflow.service.MessageService;
import com.sprintflow.service.ProjectService;
import com.sprintflow.mapper.MessageMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @MockitoBean
    private MessageMapper messageMapper;

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void sendMessage_shouldReturn201_whenAuthorized() throws Exception {

        CreateMessageRequest request = new CreateMessageRequest();
        request.setProjectId(1L);
        request.setContent("Hello team");

        Project project = new Project();
        project.setProjectId(1L);

        Chat chat = new Chat();
        chat.setChatId(1L);
        project.setChat(chat);

        Message message = new Message();
        message.setMessageId(1L);
        message.setMessageText("Hello team");
        message.setCreatedAt(LocalDateTime.now());

        when(projectService.getProjectById(1L))
                .thenReturn(project);

        when(messageService.sendMessage(anyString(), anyLong(), anyString()))
                .thenReturn(message);

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void getMessagesByProjectId_shouldReturn403_whenAuthorizationFails() throws Exception {

        doThrow(new SecurityException("User is not authorized for this project"))
                .when(projectService)
                .ensureProjectMemberOrOwner(anyLong(), anyString());

        mockMvc.perform(get("/api/messages/project/1"))
                .andExpect(status().isForbidden());
    }

}