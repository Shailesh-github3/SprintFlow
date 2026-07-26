package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import com.sprintflow.entity.PlanType;
import com.sprintflow.entity.Subscription;
import com.sprintflow.entity.User;
import com.sprintflow.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void createSubscription_shouldCreateFreePlanSubscription() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@example.com");

        Subscription saved = new Subscription();
        saved.setSubscriptionId(10L);
        saved.setUser(user);
        saved.setPlanType(PlanType.FREE);
        saved.setStartDate(LocalDate.now());
        saved.setEndDate(LocalDate.now().plusMonths(1));
        saved.setValid(true);

        when(subscriptionRepository.save(org.mockito.ArgumentMatchers.any(Subscription.class))).thenReturn(saved);

        Subscription result = subscriptionService.createSubscription(user);

        assertEquals(PlanType.FREE, result.getPlanType());
        assertEquals(user, result.getUser());
        verify(subscriptionRepository).save(org.mockito.ArgumentMatchers.any(Subscription.class));
    }

    @Test
    void getSubscriptionByUserId_shouldReturnSubscription() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@example.com");

        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(10L);
        subscription.setUser(user);
        subscription.setPlanType(PlanType.FREE);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(1));
        subscription.setValid(true);

        when(subscriptionRepository.findByUser_UserId(1L)).thenReturn(subscription);

        Subscription result = subscriptionService.getSubscriptionByUserId(1L);

        assertEquals(PlanType.FREE, result.getPlanType());
    }

    @Test
    void updateSubscription_shouldUpdateToFreePlanSuccessfully() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@example.com");

        Subscription current = new Subscription();
        current.setSubscriptionId(10L);
        current.setUser(user);
        current.setPlanType(PlanType.MONTHLY);
        current.setStartDate(LocalDate.now());
        current.setEndDate(LocalDate.now().plusMonths(1));
        current.setValid(true);

        when(subscriptionRepository.findByUser_UserId(1L)).thenReturn(current);
        when(subscriptionRepository.save(org.mockito.ArgumentMatchers.any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = subscriptionService.updateSubscription(1L, PlanType.FREE);

        assertEquals(PlanType.FREE, result.getPlanType());
        verify(subscriptionRepository).save(org.mockito.ArgumentMatchers.any(Subscription.class));
    }
}
