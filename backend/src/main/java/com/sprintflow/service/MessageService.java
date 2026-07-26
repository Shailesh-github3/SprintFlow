package com.sprintflow.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintflow.entity.Chat;
import com.sprintflow.entity.Message;
import com.sprintflow.entity.User;
import com.sprintflow.repository.MessageRepository;
import com.sprintflow.repository.UserRepository;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository, ProjectService projectService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
    }

    @Transactional
    public Message sendMessage(String senderEmail, Long projectId, String content) throws Exception {
        String resolvedSenderEmail = Objects.requireNonNull(senderEmail, "senderEmail must not be null");
        Long resolvedProjectId = Objects.requireNonNull(projectId, "projectId must not be null");
        User sender = userRepository.findByEmail(resolvedSenderEmail);
        if (sender == null) {
            throw new IllegalArgumentException("User not found");
        }
        Chat chat = projectService.getChatByProjectId(resolvedProjectId);
        Message message = new Message();
        message.setSender(sender);
        message.setChat(chat);
        message.setMessageText(content);
        message.setCreatedAt(LocalDateTime.now());
        
        Message savedMessage = messageRepository.save(message);
        if (chat.getMessages() == null) {
            chat.setMessages(new java.util.ArrayList<>());
        }
        chat.getMessages().add(savedMessage);
        return savedMessage;
    }

    @Transactional(readOnly = true)
    public Page<Message> getMessagesByProjectId(Long projectId, Pageable pageable) throws Exception {
        Long resolvedProjectId = Objects.requireNonNull(projectId, "projectId must not be null");
        Chat chat = projectService.getChatByProjectId(resolvedProjectId);
        return messageRepository.findByChat_ChatId(chat.getChatId(), pageable);
    }

}
