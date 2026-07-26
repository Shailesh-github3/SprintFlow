package com.sprintflow.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintflow.entity.PlanType;
import com.sprintflow.entity.Subscription;
import com.sprintflow.entity.User;
import com.sprintflow.repository.SubscriptionRepository;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public Subscription createSubscription(User user) {
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(1));
        subscription.setValid(true);
        subscription.setPlanType(PlanType.FREE);

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription getSubscriptionByUserId(Long userId) {
        Subscription subscription = subscriptionRepository.findByUser_UserId(userId);
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription not found for user with ID: " + userId);
        }
        if (!isSubscriptionValid(subscription)) {
            subscription.setPlanType(PlanType.FREE);
            subscription.setStartDate(LocalDate.now());
            subscription.setEndDate(LocalDate.now().plusMonths(1));
            subscription.setValid(true);
            subscriptionRepository.save(subscription);
        }
        return subscription;
    }

    @Transactional
    public Subscription updateSubscription(Long userId, PlanType planType) {
        Subscription subscription = subscriptionRepository.findByUser_UserId(userId);
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription not found for user with ID: " + userId);
        }

        subscription.setPlanType(planType);
        subscription.setStartDate(LocalDate.now());
        if (planType == PlanType.FREE) {
            subscription.setEndDate(LocalDate.now().plusMonths(1));
        } else if (planType == PlanType.MONTHLY) {
            subscription.setEndDate(LocalDate.now().plusMonths(1));
        } else if (planType == PlanType.YEARLY) {
            subscription.setEndDate(LocalDate.now().plusYears(1));
        } else {
            throw new IllegalArgumentException("Invalid plan type: " + planType);
        }
        subscription.setValid(true);

        return subscriptionRepository.save(subscription);
    }

    public boolean isSubscriptionValid(Subscription subscription) {
        if (subscription.getPlanType().equals(PlanType.FREE)) {
            return true;
        }

        LocalDate endDate = subscription.getEndDate();
        LocalDate currentDate = LocalDate.now();

        return endDate != null && (endDate.isAfter(currentDate) || endDate.isEqual(currentDate));
    }

}