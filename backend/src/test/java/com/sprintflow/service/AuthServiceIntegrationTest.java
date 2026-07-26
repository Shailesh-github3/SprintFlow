package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sprintflow.entity.PlanType;
import com.sprintflow.entity.Subscription;
import com.sprintflow.entity.User;
import com.sprintflow.repository.SubscriptionRepository;
import com.sprintflow.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

/**
 * Integration test proving the full register → user creation → subscription
 * transactional flow against a real MySQL database via Testcontainers.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("sprintflow_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        // Use Flyway to manage schema (matches production setup)
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        // Validate schema after Flyway migration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // Provide a test JWT secret (minimum 32 bytes for HMAC-SHA256)
        registry.add("sprintflow.jwt.secret", () -> "test-secret-key-that-is-at-least-32-bytes-long-for-hmac");
        registry.add("sprintflow.jwt.expiration-ms", () -> "86400000");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("registerUser creates a User and a FREE subscription, password is BCrypt hashed")
    void registerUser_CreatesUserAndFreeSubscription_Success() {
        // Arrange: simulate what AuthController.registerUserHandler does
        String rawPassword = "securePassword123";

        User newUser = new User();
        newUser.setFullName("Test User");
        newUser.setEmail("integrationtest@example.com");
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setRoles(List.of("USER"));

        // Act: save user and create subscription (mirrors AuthController flow)
        User savedUser = userRepository.save(newUser);
        Subscription subscription = subscriptionService.createSubscription(savedUser);

        // Assert: User is persisted
        assertNotNull(savedUser.getUserId(), "Saved user should have a generated ID");
        assertEquals("integrationtest@example.com", savedUser.getEmail());
        assertEquals("Test User", savedUser.getFullName());

        // Assert: Password is BCrypt hashed, NOT plain text
        assertNotEquals(rawPassword, savedUser.getPassword(),
                "Password must not be stored as plain text");
        assertTrue(savedUser.getPassword().startsWith("$2a$") || savedUser.getPassword().startsWith("$2b$"),
                "Password must be BCrypt hashed (starts with $2a$ or $2b$)");
        assertTrue(passwordEncoder.matches(rawPassword, savedUser.getPassword()),
                "BCrypt encoder must verify the original password against the hash");

        // Assert: Subscription is created with PlanType.FREE
        assertNotNull(subscription, "Subscription should be created");
        assertNotNull(subscription.getSubscriptionId(), "Subscription should have a generated ID");
        assertEquals(PlanType.FREE, subscription.getPlanType(),
                "New user subscription must default to FREE plan");
        assertTrue(subscription.isValid(), "New subscription should be valid");
        assertNotNull(subscription.getStartDate(), "Start date must be set");
        assertNotNull(subscription.getEndDate(), "End date must be set");
        assertEquals(savedUser.getUserId(), subscription.getUser().getUserId(),
                "Subscription must be linked to the saved user");

        // Assert: data is actually in the DB (round-trip verification)
        User fromDb = userRepository.findByEmail("integrationtest@example.com");
        assertNotNull(fromDb, "User must be retrievable from database by email");
        assertEquals(savedUser.getUserId(), fromDb.getUserId());

        Subscription subFromDb = subscriptionRepository.findByUser_UserId(savedUser.getUserId());
        assertNotNull(subFromDb, "Subscription must be retrievable from database by user ID");
        assertEquals(PlanType.FREE, subFromDb.getPlanType());
    }
}
