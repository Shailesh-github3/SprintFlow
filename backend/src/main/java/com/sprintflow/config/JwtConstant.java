package com.sprintflow.config;

public class JwtConstant {

    public static final String JWT_SECRET = System.getenv().getOrDefault("SPRINGFLOW_JWT_SECRET", "springflow-dev-secret-key-change-me-in-production-1234567890");
    public static final String JWT_HEADER = "Authorization";
    public static final long JWT_EXPIRATION = 86400000; // 1 day in milliseconds

}
