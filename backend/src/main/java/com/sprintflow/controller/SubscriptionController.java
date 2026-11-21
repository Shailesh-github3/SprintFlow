package com.sprintflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sprintflow.dto.SubscriptionDTO;
import com.sprintflow.entity.Subscription;
import com.sprintflow.entity.User;
import com.sprintflow.mapper.SubscriptionMapper;
import com.sprintflow.service.SubscriptionService;
import com.sprintflow.service.UserService;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final SubscriptionMapper subscriptionMapper;

    public SubscriptionController(SubscriptionService subscriptionService, UserService userService, SubscriptionMapper subscriptionMapper) {
        this.subscriptionService = subscriptionService;
        this.userService = userService;
        this.subscriptionMapper = subscriptionMapper;
    }

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionDTO> createSubscription() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(currentUserEmail);
        Subscription existing = subscriptionService.getSubscriptionByUserId(user.getUserId());
        if (existing == null) {
            existing = subscriptionService.createSubscription(user);
        }
        return ResponseEntity.ok(subscriptionMapper.toDto(existing));
    }

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionDTO> getUserSubscription() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(currentUserEmail);
        Subscription subscription = subscriptionService.getSubscriptionByUserId(user.getUserId());
        return ResponseEntity.ok(subscriptionMapper.toDto(subscription));
    }
}
