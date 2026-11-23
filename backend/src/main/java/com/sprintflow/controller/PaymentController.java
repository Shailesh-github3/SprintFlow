package com.sprintflow.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sprintflow.dto.SubscriptionDTO;
import com.sprintflow.entity.PlanType;
import com.sprintflow.entity.Subscription;
import com.sprintflow.entity.User;
import com.sprintflow.mapper.SubscriptionMapper;
import com.sprintflow.request.PaymentVerificationRequest;
import com.sprintflow.response.PaymentLinkResponse;
import com.sprintflow.service.PaymentService;
import com.sprintflow.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;
    private final SubscriptionMapper subscriptionMapper;

    public PaymentController(PaymentService paymentService, UserService userService, SubscriptionMapper subscriptionMapper) {
        this.paymentService = paymentService;
        this.userService = userService;
        this.subscriptionMapper = subscriptionMapper;
    }

    @PostMapping("/{planType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentLinkResponse> createPaymentLink(@PathVariable PlanType planType) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        PaymentLinkResponse response = paymentService.createPaymentLink(currentEmail, planType)
                .orElseThrow(() -> new IllegalStateException("Failed to create payment link with payment provider"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionDTO> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request) throws Exception {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(currentEmail);
        Subscription subscription = paymentService.completePayment(
                user.getUserId(),
                request.getPlanType(),
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                LocalDateTime.now()
        );
        return ResponseEntity.ok(subscriptionMapper.toDto(subscription));
    }
}
