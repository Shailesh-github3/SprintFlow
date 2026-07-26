package com.sprintflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sprintflow.jwt")
public record JwtProperties(String secret, long expirationMs) {
    
}