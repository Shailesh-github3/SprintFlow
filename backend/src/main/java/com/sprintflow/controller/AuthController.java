package com.sprintflow.controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.sprintflow.repository.UserRepository;
import com.sprintflow.response.AuthResponse;
import com.sprintflow.config.JwtProvider;
import com.sprintflow.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import com.sprintflow.service.SubscriptionService;
import jakarta.validation.Valid;
import com.sprintflow.request.LoginRequest;
import com.sprintflow.request.RegisterRequest;

import java.util.List;



@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, SubscriptionService subscriptionService, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionService = subscriptionService;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/register")    
    public ResponseEntity<?> registerUserHandler(@Valid @RequestBody RegisterRequest request) throws Exception {
        // Check if the user already exists
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new Exception("User with email " + request.getEmail() + " already exists.");
        }

        User newUser = new User();
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setEmail(request.getEmail());
        newUser.setFullName(request.getFullName());
        newUser.setRoles(List.of("USER"));
        User savedUser = userRepository.save(newUser);

        subscriptionService.createSubscription(savedUser);

        Authentication authenication = new UsernamePasswordAuthenticationToken(
            savedUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authenication);

        String jwt = jwtProvider.generateToken(authenication);

        AuthResponse response = new AuthResponse();
        response.setJwt(jwt);
        response.setMessage("User created successfully");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginHandler(@Valid @RequestBody LoginRequest request) throws Exception {
        User existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser == null || !passwordEncoder.matches(request.getPassword(), existingUser.getPassword())) {
            throw new Exception("Invalid email or password.");
        }

        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        if (existingUser.getRoles() != null) {
            for (String role : existingUser.getRoles()) {
                authorities.add(new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role));
            }
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        Authentication authenication = new UsernamePasswordAuthenticationToken(
            existingUser.getEmail(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authenication);

        String jwt = jwtProvider.generateToken(authenication);

        AuthResponse response = new AuthResponse();
        response.setJwt(jwt);
        response.setMessage("Login successful");

        return ResponseEntity.ok(response);
    }

}