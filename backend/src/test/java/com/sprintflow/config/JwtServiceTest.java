package com.sprintflow.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class JwtServiceTest {

    @Test
    void generateToken_shouldCreateTokenWithSubjectAndRoles() {
        JwtProperties properties = new JwtProperties("springflow-test-secret-key-1234567890", 86_400_000L);
        JwtService jwtService = new JwtService(properties);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "alice@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtService.generateToken(authentication);

        assertNotNull(token);
        assertEquals("alice@example.com", jwtService.parseClaims(token).getSubject());
        
        @SuppressWarnings("unchecked")
        List<String> roles = jwtService.parseClaims(token).get("roles", List.class);
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("ROLE_USER", roles.get(0));
    }
}