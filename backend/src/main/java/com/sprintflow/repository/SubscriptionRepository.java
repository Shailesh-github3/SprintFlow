package com.sprintflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sprintflow.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Subscription findByUser_UserId(Long userId);

}
