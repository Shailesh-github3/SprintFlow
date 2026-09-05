package com.sprintflow.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.exception.ApiErrorResponse;

/**
 * Simple in-memory rate limiter for the public auth endpoints
 * (brute-force protection for login/register). Sliding window:
 * {@value #MAX_ATTEMPTS} requests per {@value #WINDOW_SECONDS} seconds per
 * client IP. Rejected requests get 429 with the standard error shape.
 *
 * <p>Adequate for a single-instance demo. A multi-instance deployment would
 * need a shared store (e.g. Redis) — explicitly out of scope for v1.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    static final int MAX_ATTEMPTS = 20;
    static final long WINDOW_SECONDS = 60;
    private static final long WINDOW_MS = WINDOW_SECONDS * 1000;
    private static final int MAX_TRACKED_IPS = 10_000;

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Deque<Long>> attemptsByIp = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }
        if (isRateLimited(clientIp(request))) {
            ApiErrorResponse body = new ApiErrorResponse(
                    LocalDateTime.now(),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests",
                    "Too many attempts. Please try again in a minute.",
                    request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isRateLimited(String ip) {
        if (attemptsByIp.size() > MAX_TRACKED_IPS) {
            attemptsByIp.clear();
        }
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = attemptsByIp.computeIfAbsent(ip, key -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_ATTEMPTS) {
                return true;
            }
            timestamps.addLast(now);
            return false;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
