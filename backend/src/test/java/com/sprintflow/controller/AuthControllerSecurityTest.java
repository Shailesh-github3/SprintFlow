package com.sprintflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.config.JwtProvider;
import com.sprintflow.config.JwtService;
import com.sprintflow.config.JwtTokenValidator;
import com.sprintflow.entity.User;
import com.sprintflow.repository.UserRepository;
import com.sprintflow.request.RegisterRequest;
import com.sprintflow.service.SubscriptionService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private JwtProvider jwtProvider;

    // Added for Spring Boot 3.5 Security Context
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @Test
    void register_shouldReturn201_whenValidRequest() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setFullName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(null);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded");

        User savedUser = new User();
        savedUser.setUserId(1L);
        savedUser.setFullName("Alice");
        savedUser.setEmail("alice@example.com");
        savedUser.setPassword("encoded");
        savedUser.setRoles(java.util.List.of("USER"));

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        when(jwtProvider.generateToken(any()))
                .thenReturn("mock-jwt");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jwt").value("mock-jwt"))
                .andExpect(jsonPath("$.message").value("User created successfully"));
    }
}