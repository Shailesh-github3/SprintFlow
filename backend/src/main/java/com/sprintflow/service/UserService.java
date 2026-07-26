package com.sprintflow.service;

import com.sprintflow.repository.UserRepository;
import com.sprintflow.entity.User;
import java.util.Objects;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new BadCredentialsException("User not found with email: " + email);
        }
        return user;
    }

    @Transactional
    public User updateUserProjectSize(User user, int newProjectSize) {
        user.setProjectSize(user.getProjectSize() + newProjectSize);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUserByUserId(Long userId) {
        Long resolvedUserId = Objects.requireNonNull(userId, "userId must not be null");
        return userRepository.findById(resolvedUserId)
                .orElseThrow(() -> new RuntimeException("User not found with userId: " + userId));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail);
    }

}