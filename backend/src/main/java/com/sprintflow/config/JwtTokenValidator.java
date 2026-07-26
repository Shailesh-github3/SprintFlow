// File: src/main/java/com/sprintflow/config/JwtTokenValidator.java
package com.sprintflow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtTokenValidator extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtTokenValidator(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = request.getHeader(JwtConstant.JWT_HEADER);

        if (jwt == null || !jwt.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = jwt.substring(7);

        try {
            Claims claims = jwtService.parseClaims(jwt);
            String email = claims.getSubject();
            
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            List<GrantedAuthority> authorities;
            if (roles != null) {
                authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                    .collect(Collectors.toList());
            } else {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            }
            
            Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

        } catch (ExpiredJwtException e) {
            throw new BadCredentialsException("Token has expired", e);
        } catch (MalformedJwtException e) {
            throw new BadCredentialsException("Malformed token", e);
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid token", e);
        }

        filterChain.doFilter(request, response);
    }
}