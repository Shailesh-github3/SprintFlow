package com.sprintflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sprintflow.entity.Chat;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    // You can define custom query methods here if needed


}
