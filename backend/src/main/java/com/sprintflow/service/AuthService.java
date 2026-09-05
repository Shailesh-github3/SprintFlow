package com.sprintflow.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintflow.entity.User;
import com.sprintflow.exception.DuplicateResourceException;
import com.sprintflow.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionService subscriptionService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       SubscriptionService subscriptionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionService = subscriptionService;
    }

    /**
     * Registers a new user and creates their FREE subscription atomically.
     * If subscription creation fails, the user insert rolls back (no orphan users).
     */
    @Transactional
    public User register(String fullName, String email, String rawPassword) {
        if (userRepository.findByEmail(email) != null) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }
        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setRoles(List.of("USER"));
        User savedUser = userRepository.save(newUser);
        subscriptionService.createSubscription(savedUser);
        return savedUser;
    }
}
