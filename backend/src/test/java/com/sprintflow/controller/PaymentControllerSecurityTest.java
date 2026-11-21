package com.sprintflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;

import com.sprintflow.config.JwtService;
import com.sprintflow.config.JwtTokenValidator;
import com.sprintflow.dto.SubscriptionDTO;
import com.sprintflow.entity.PlanType;
import com.sprintflow.entity.Subscription;
import com.sprintflow.entity.User;
import com.sprintflow.mapper.SubscriptionMapper;
import com.sprintflow.response.PaymentLinkResponse;
import com.sprintflow.service.PaymentService;
import com.sprintflow.service.UserService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SubscriptionMapper subscriptionMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @Test
    @DisplayName("reports a server error when the payment provider call fails")
    void createPaymentLink_whenProviderFails_returnsServerError() throws Exception {
        when(paymentService.createPaymentLink("alice@example.com", PlanType.MONTHLY))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/payments/MONTHLY").with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    @DisplayName("returns a payment link for authenticated users")
    void createPaymentLink_withAuthentication_returnsPaymentLink() throws Exception {
        PaymentLinkResponse response = new PaymentLinkResponse("https://rzp.io/l/monthly", "plink_123");
        when(paymentService.createPaymentLink("alice@example.com", PlanType.MONTHLY))
                .thenReturn(Optional.of(response));

        mockMvc.perform(post("/api/payments/MONTHLY").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentLink").value("https://rzp.io/l/monthly"))
                .andExpect(jsonPath("$.paymentLinkId").value("plink_123"));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    @DisplayName("verifies payment signature and returns updated subscription")
    void verifyPayment_withValidSignature_returnsSubscription() throws Exception {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("alice@example.com");

        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(10L);
        subscription.setPlanType(PlanType.MONTHLY);

        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(10L);
        dto.setPlanType(PlanType.MONTHLY);

        when(userService.findByEmail("alice@example.com")).thenReturn(user);
        when(paymentService.completePayment(eq(1L), eq(PlanType.MONTHLY), eq("order_123"), eq("pay_123"), eq("sig_123"), any(LocalDateTime.class)))
                .thenReturn(subscription);
        when(subscriptionMapper.toDto(subscription)).thenReturn(dto);

        String payload = """
                {
                    "planType": "MONTHLY",
                    "razorpayOrderId": "order_123",
                    "razorpayPaymentId": "pay_123",
                    "razorpaySignature": "sig_123"
                }
                """;

        mockMvc.perform(post("/api/payments/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(10))
                .andExpect(jsonPath("$.planType").value("MONTHLY"));
    }
}
