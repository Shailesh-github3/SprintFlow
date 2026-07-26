package com.sprintflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sprintflow.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByChat_ChatId(Long chatId, Pageable pageable);

}
