package com.sprintflow.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import com.sprintflow.config.JwtService;
import com.sprintflow.config.JwtTokenValidator;
import com.sprintflow.entity.PlanType;
import com.sprintflow.response.PaymentLinkResponse;
import com.sprintflow.service.PaymentService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
}