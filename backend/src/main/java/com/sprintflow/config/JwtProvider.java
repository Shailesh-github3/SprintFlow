package com.sprintflow.config;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private final JwtService jwtService;

    public JwtProvider(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String generateToken(Authentication auth) {
        return jwtService.generateToken(auth);
    }

}
