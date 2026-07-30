package com.sprintflow.controller;

import java.time.Duration;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.sprintflow.repository.UserRepository;
import com.sprintflow.config.JwtProvider;
import com.sprintflow.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import com.sprintflow.service.SubscriptionService;
import com.sprintflow.config.JwtConstant;
import com.sprintflow.config.JwtProperties;
import jakarta.validation.Valid;
import com.sprintflow.request.LoginRequest;
import com.sprintflow.request.RegisterRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, SubscriptionService subscriptionService, JwtProvider jwtProvider, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionService = subscriptionService;
        this.jwtProvider = jwtProvider;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")    
    public ResponseEntity<?> registerUserHandler(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
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
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, buildJwtCookie(jwt, servletRequest).toString());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginHandler(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
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
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, buildJwtCookie(jwt, servletRequest).toString());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutHandler(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, clearJwtCookie(servletRequest).toString());
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    private ResponseCookie buildJwtCookie(String jwt, HttpServletRequest request) {
        return ResponseCookie.from(JwtConstant.JWT_COOKIE, jwt)
                .httpOnly(true)
                .secure(isSecureRequest(request))
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.expirationMs()))
                .build();
    }

    private ResponseCookie clearJwtCookie(HttpServletRequest request) {
        return ResponseCookie.from(JwtConstant.JWT_COOKIE, "")
                .httpOnly(true)
                .secure(isSecureRequest(request))
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return request.isSecure() || "https".equalsIgnoreCase(forwardedProto);
    }

}