package com.sprintflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.config.JwtProperties;
import com.sprintflow.config.JwtProvider;
import com.sprintflow.config.JwtService;
import com.sprintflow.config.JwtTokenValidator;
import com.sprintflow.entity.User;
import com.sprintflow.exception.DuplicateResourceException;
import com.sprintflow.repository.UserRepository;
import com.sprintflow.request.LoginRequest;
import com.sprintflow.request.RegisterRequest;
import com.sprintflow.service.AuthService;

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
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtProperties jwtProperties;

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

        User savedUser = new User();
        savedUser.setUserId(1L);
        savedUser.setFullName("Alice");
        savedUser.setEmail("alice@example.com");
        savedUser.setPassword("encoded");
        savedUser.setRoles(java.util.List.of("USER"));

        when(authService.register("Alice", "alice@example.com", "password123"))
                .thenReturn(savedUser);

        when(jwtProvider.generateToken(any()))
                .thenReturn("mock-jwt");
        when(jwtProperties.expirationMs())
                .thenReturn(86400000L);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("sprintflow_jwt")));
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setFullName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        when(authService.register("Alice", "alice@example.com", "password123"))
                .thenThrow(new DuplicateResourceException("An account with this email already exists."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_shouldReturn401_whenPasswordIsWrong() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("wrongpassword");

        User existingUser = new User();
        existingUser.setUserId(1L);
        existingUser.setEmail("alice@example.com");
        existingUser.setPassword("encoded");
        existingUser.setRoles(java.util.List.of("USER"));

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(existingUser);
        when(passwordEncoder.matches("wrongpassword", "encoded"))
                .thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
