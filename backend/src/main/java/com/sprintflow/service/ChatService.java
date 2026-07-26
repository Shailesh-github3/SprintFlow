package com.sprintflow.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintflow.entity.Chat;
import com.sprintflow.repository.ChatRepository;

@Service
public class ChatService {

    private final ChatRepository chatRepository;

    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Transactional
    public Chat createChat(Chat chat) {
        return chatRepository.save(chat);
    }

}
