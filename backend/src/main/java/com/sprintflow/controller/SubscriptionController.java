package com.sprintflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sprintflow.entity.PlanType;
import com.sprintflow.entity.User;
import com.sprintflow.service.SubscriptionService;
import com.sprintflow.service.UserService;
import com.sprintflow.mapper.SubscriptionMapper;

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
    public ResponseEntity<?> createSubscription() {
        try {
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.findByEmail(currentUserEmail);
            subscriptionService.createSubscription(user);
            return ResponseEntity.ok("Subscription created successfully for user: " + currentUserEmail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserSubscription() {
        try {
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.findByEmail(currentUserEmail);
            return ResponseEntity.ok(subscriptionMapper.toDto(subscriptionService.getSubscriptionByUserId(user.getUserId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/update/{planType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUserSubscription(@PathVariable String planType) {
        try {
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.findByEmail(currentUserEmail);
            subscriptionService.updateSubscription(user.getUserId(), PlanType.valueOf(planType.toUpperCase()));
            return ResponseEntity.ok("Subscription updated successfully for user: " + currentUserEmail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}