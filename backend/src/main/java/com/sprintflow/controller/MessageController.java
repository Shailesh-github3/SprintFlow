package com.sprintflow.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sprintflow.entity.Chat;
import com.sprintflow.entity.Message;
import com.sprintflow.service.MessageService;
import com.sprintflow.service.ProjectService;
import com.sprintflow.request.CreateMessageRequest;
import jakarta.validation.Valid;
import com.sprintflow.mapper.MessageMapper;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final ProjectService projectService;
    private final MessageMapper messageMapper;

    public MessageController(MessageService messageService, ProjectService projectService, MessageMapper messageMapper) {
        this.messageService = messageService;
        this.projectService = projectService;
        this.messageMapper = messageMapper;
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody CreateMessageRequest request) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.ensureProjectMemberOrOwner(request.getProjectId(), currentUserEmail);
        
        Chat chat = projectService.getProjectById(request.getProjectId()).getChat();
        if (chat == null) {
            throw new IllegalArgumentException("Chat not found for the project");
        }
        Message message = messageService.sendMessage(currentUserEmail, request.getProjectId(), request.getContent());
        return ResponseEntity.status(201).body(messageMapper.toDto(message));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMessagesByProjectId(@PathVariable Long projectId,
                                                     @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.ensureProjectMemberOrOwner(projectId, currentUserEmail);
        Page<Message> messagePage = messageService.getMessagesByProjectId(projectId, pageable);
        return ResponseEntity.ok(messagePage.map(messageMapper::toDto));
    }

    @GetMapping("/chat/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMessagesByChatId(@PathVariable Long projectId,
                                                  @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.ensureProjectMemberOrOwner(projectId, currentUserEmail);
        Page<Message> messagePage = messageService.getMessagesByProjectId(projectId, pageable);
        return ResponseEntity.ok(messagePage.map(messageMapper::toDto));
    }
}