package com.sprintflow.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        filter = new RateLimitFilter(objectMapper);
        chain = mock(FilterChain.class);
    }

    @Test
    void nonAuthPaths_areNeverLimited() throws Exception {
        HttpServletRequest request = request("/api/projects", "10.0.0.1");
        for (int i = 0; i < RateLimitFilter.MAX_ATTEMPTS + 5; i++) {
            filter.doFilter(request, mock(HttpServletResponse.class), chain);
        }
        verify(chain, times(RateLimitFilter.MAX_ATTEMPTS + 5)).doFilter(any(), any());
    }

    @Test
    void authPaths_rejectThe21stRequestWithinAMinute() throws Exception {
        HttpServletRequest request = request("/api/auth/login", "10.0.0.2");
        for (int i = 0; i < RateLimitFilter.MAX_ATTEMPTS; i++) {
            filter.doFilter(request, mock(HttpServletResponse.class), chain);
        }
        verify(chain, times(RateLimitFilter.MAX_ATTEMPTS)).doFilter(any(), any());

        StringWriter body = new StringWriter();
        HttpServletResponse rejected = mock(HttpServletResponse.class);
        when(rejected.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilter(request, rejected, chain);

        verify(chain, times(RateLimitFilter.MAX_ATTEMPTS)).doFilter(any(), any());
        verify(rejected).setStatus(429);
        assertTrue(body.toString().contains("Too Many Requests"));
    }

    @Test
    void limits_arePerClientIp() throws Exception {
        HttpServletRequest alice = request("/api/auth/login", "10.0.0.3");
        for (int i = 0; i < RateLimitFilter.MAX_ATTEMPTS; i++) {
            filter.doFilter(alice, mock(HttpServletResponse.class), chain);
        }
        filter.doFilter(request("/api/auth/login", "10.0.0.4"), mock(HttpServletResponse.class), chain);
        verify(chain, times(RateLimitFilter.MAX_ATTEMPTS + 1)).doFilter(any(), any());
    }

    @Test
    void forwardedHeader_isUsedAsClientIdentity() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader(anyString())).thenReturn("9.9.9.9");
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        for (int i = 0; i < RateLimitFilter.MAX_ATTEMPTS; i++) {
            filter.doFilter(request, mock(HttpServletResponse.class), chain);
        }
        HttpServletResponse rejected = mock(HttpServletResponse.class);
        when(rejected.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        filter.doFilter(request, rejected, chain);
        verify(rejected).setStatus(429);
        verify(chain, times(RateLimitFilter.MAX_ATTEMPTS)).doFilter(any(), any());
    }

    private HttpServletRequest request(String uri, String ip) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getRemoteAddr()).thenReturn(ip);
        return request;
    }
}
