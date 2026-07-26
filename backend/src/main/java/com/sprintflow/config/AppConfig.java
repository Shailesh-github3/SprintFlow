// File: src/main/java/com/sprintflow/config/AppConfig.java
package com.sprintflow.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class AppConfig {

    private final JwtTokenValidator jwtTokenValidator;
    private final CorsProperties corsProperties;

    public AppConfig(JwtTokenValidator jwtTokenValidator, CorsProperties corsProperties) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.corsProperties = corsProperties;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(Management -> Management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtTokenValidator, UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource((corsConfigurationSource())));

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(corsProperties.allowedOrigins() == null || corsProperties.allowedOrigins().isEmpty()
                    ? List.of("http://localhost:3000", "http://localhost:5173", "http://localhost:4200")
                    : corsProperties.allowedOrigins());
            config.setAllowedMethods(corsProperties.allowedMethods() == null || corsProperties.allowedMethods().isEmpty()
                    ? List.of("GET", "POST", "PUT", "PATCH", "DELETE")
                    : corsProperties.allowedMethods());
            config.setAllowedHeaders(corsProperties.allowedHeaders() == null || corsProperties.allowedHeaders().isEmpty()
                    ? List.of("*")
                    : corsProperties.allowedHeaders());
            config.setExposedHeaders(List.of("Authorization"));
            config.setAllowCredentials(true);
            config.setMaxAge(3600L);
            return config;
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}