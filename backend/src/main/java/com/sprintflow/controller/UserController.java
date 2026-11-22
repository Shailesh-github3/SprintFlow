package com.sprintflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sprintflow.dto.DashboardStatsDTO;
import com.sprintflow.dto.UserDTO;
import com.sprintflow.entity.User;
import com.sprintflow.exception.ResourceNotFoundException;
import com.sprintflow.mapper.UserMapper;
import com.sprintflow.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUser() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByEmail(currentUserEmail);
        if (user == null) {
            throw new ResourceNotFoundException("Current user not found");
        }
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @GetMapping("/dashboard-stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByEmail(currentUserEmail);
        if (user == null) {
            throw new ResourceNotFoundException("Current user not found");
        }
        DashboardStatsDTO stats = userService.getDashboardStats(user);
        return ResponseEntity.ok(stats);
    }
}
