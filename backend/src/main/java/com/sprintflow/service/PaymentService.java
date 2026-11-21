package com.sprintflow.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.sprintflow.entity.PlanType;
import com.sprintflow.entity.Subscription;
import com.sprintflow.entity.User;
import com.sprintflow.response.PaymentLinkResponse;

@Service
public class PaymentService {

    private static final long PAYMENT_SESSION_EXPIRY_MINUTES = 15L;

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final String apiKey;
    private final String apiSecret;
    private final String frontendUrl;

    public PaymentService(UserService userService,
                          SubscriptionService subscriptionService,
                          @Value("${razorpay.keyId}") String apiKey,
                          @Value("${razorpay.keySecret}") String apiSecret) {
        this(userService, subscriptionService, apiKey, apiSecret, "http://localhost:5173");
    }

    public PaymentService(UserService userService,
                          SubscriptionService subscriptionService,
                          @Value("${razorpay.keyId}") String apiKey,
                          @Value("${razorpay.keySecret}") String apiSecret,
                          @Value("${sprintflow.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.frontendUrl = frontendUrl;
    }

    public Optional<PaymentLinkResponse> createPaymentLink(String userEmail, PlanType planType) {
        User user = userService.getUserByEmail(userEmail);
        long amountInPaise = Math.round(planType.getPrice() * 100);

        try {
            RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);
            JSONObject paymentLinkRequest = buildPaymentLinkRequest(user, planType, amountInPaise);
            PaymentLink payment = razorpayClient.paymentLink.create(paymentLinkRequest);

            String paymentLinkId = payment.get("id");
            String paymentLink = payment.get("short_url");
            return Optional.of(new PaymentLinkResponse(paymentLink, paymentLinkId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Subscription completePayment(Long userId,
                                        PlanType planType,
                                        String razorpayOrderId,
                                        String razorpayPaymentId,
                                        String razorpaySignature,
                                        LocalDateTime sessionCreatedAt) throws Exception {
        validateSessionExpiry(sessionCreatedAt);
        validateSignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
        return subscriptionService.updateSubscription(userId, planType);
    }

    String generateSignature(String razorpayOrderId, String razorpayPaymentId) throws Exception {
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    private void validateSessionExpiry(LocalDateTime sessionCreatedAt) {
        if (sessionCreatedAt.plusMinutes(PAYMENT_SESSION_EXPIRY_MINUTES).isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Payment session has expired");
        }
    }

    private void validateSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) throws Exception {
        if (!generateSignature(razorpayOrderId, razorpayPaymentId).equals(razorpaySignature)) {
            throw new SecurityException("Invalid payment signature");
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

        paymentLinkRequest.put("callback_url", frontendUrl + "/upgrade-plan/success?planType=" + planType.name());

        return paymentLinkRequest;
    }
}