package com.sprintflow.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.sprintflow.entity.PlanType;
import com.sprintflow.entity.User;
import com.sprintflow.response.PaymentLinkResponse;
import com.sprintflow.service.UserService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Value("${razorpay.keyId}")
    private String apiKey;

    @Value("${razorpay.keySecret}")
    private String apiSecret;

    private final UserService userService;

    public PaymentController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/{planType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createPaymentLink(@PathVariable PlanType planType) {

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByEmail(currentEmail);
        long amountInPaise = Math.round(planType.getPrice() * 100);

        try {
            RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);

            JSONObject paymentLinkRequest = buildPaymentLinkRequest(user, planType, amountInPaise);

            PaymentLink payment = razorpayClient.paymentLink.create(paymentLinkRequest);

            String paymentLinkId = payment.get("id");
            String paymentLink = payment.get("short_url");

            PaymentLinkResponse response = new PaymentLinkResponse(paymentLink, paymentLinkId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error creating payment link: " + e.getMessage());
        }
    }

    private JSONObject buildPaymentLinkRequest(User user, PlanType planType, long amountInPaise) {

        JSONObject paymentLinkRequest = new JSONObject();
        paymentLinkRequest.put("amount", amountInPaise);
        paymentLinkRequest.put("currency", "INR");
        paymentLinkRequest.put("description", "Payment for " + planType.name() + " plan");

        JSONObject customer = new JSONObject();
        customer.put("name", user.getFullName());
        customer.put("email", user.getEmail());
        paymentLinkRequest.put("customer", customer);

        JSONObject notify = new JSONObject();
        notify.put("email", true);
        paymentLinkRequest.put("notify", notify);

        paymentLinkRequest.put(
                "callback_url",
                "https://localhost:5173/upgrade-plan/success?planType=" + planType.name()
        );

        return paymentLinkRequest;
    }
}