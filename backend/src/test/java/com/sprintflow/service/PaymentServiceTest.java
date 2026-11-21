package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import com.razorpay.PaymentLink;
import com.razorpay.PaymentLinkClient;
import com.razorpay.RazorpayClient;
import com.sprintflow.entity.PlanType;
import com.sprintflow.entity.Subscription;
import com.sprintflow.entity.User;
import com.sprintflow.response.PaymentLinkResponse;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private SubscriptionService subscriptionService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(userService, subscriptionService,
                "rzp_test_key", "payment-signature-secret-12345678901234567890");
    }

    @Nested
    @DisplayName("Payment link creation")
    class PaymentLinkCreationTests {

        @Test
        @DisplayName("creates a monthly payment link with the expected Razorpay payload")
        void createPaymentLink_monthlyPlan_success() throws Exception {
            // Given
            User user = buildUser();
            when(userService.getUserByEmail("alice@example.com")).thenReturn(user);

            PaymentLink razorpayPaymentLink = mock(PaymentLink.class);
            PaymentLinkClient paymentLinkClient = mock(PaymentLinkClient.class);

            try (MockedConstruction<RazorpayClient> ignored = mockConstruction(RazorpayClient.class, (client, context) -> {
                client.paymentLink = paymentLinkClient;
                when(paymentLinkClient.create(any(JSONObject.class))).thenReturn(razorpayPaymentLink);
                when(razorpayPaymentLink.get("id")).thenReturn("plink_monthly_123");
                when(razorpayPaymentLink.get("short_url")).thenReturn("https://rzp.io/l/monthly");
            })) {
                ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);

                // When
                Optional<PaymentLinkResponse> response = paymentService.createPaymentLink("alice@example.com", PlanType.MONTHLY);

                // Then
                assertTrue(response.isPresent());
                assertEquals("https://rzp.io/l/monthly", response.get().getPaymentLink());
                assertEquals("plink_monthly_123", response.get().getPaymentLinkId());

                verify(paymentLinkClient).create(requestCaptor.capture());
                JSONObject request = requestCaptor.getValue();
                assertEquals(999L, request.getLong("amount"));
                assertEquals("INR", request.getString("currency"));
                assertEquals("Payment for MONTHLY plan", request.getString("description"));
                assertEquals("alice@example.com", request.getJSONObject("customer").getString("email"));
                assertEquals("http://localhost:5173/upgrade-plan/success?planType=MONTHLY", request.getString("callback_url"));
                verify(razorpayPaymentLink, times(1)).get("id");
                verify(razorpayPaymentLink, times(1)).get("short_url");
            }
        }

        @Test
        @DisplayName("creates a yearly payment link with the expected Razorpay payload")
        void createPaymentLink_yearlyPlan_success() throws Exception {
            // Given
            User user = buildUser();
            when(userService.getUserByEmail("alice@example.com")).thenReturn(user);

            PaymentLink razorpayPaymentLink = mock(PaymentLink.class);
            PaymentLinkClient paymentLinkClient = mock(PaymentLinkClient.class);

            try (MockedConstruction<RazorpayClient> ignored = mockConstruction(RazorpayClient.class, (client, context) -> {
                client.paymentLink = paymentLinkClient;
                when(paymentLinkClient.create(any(JSONObject.class))).thenReturn(razorpayPaymentLink);
                when(razorpayPaymentLink.get("id")).thenReturn("plink_yearly_123");
                when(razorpayPaymentLink.get("short_url")).thenReturn("https://rzp.io/l/yearly");
            })) {
                // When
                Optional<PaymentLinkResponse> response = paymentService.createPaymentLink("alice@example.com", PlanType.YEARLY);

                // Then
                assertTrue(response.isPresent());
                assertEquals("https://rzp.io/l/yearly", response.get().getPaymentLink());
                assertEquals("plink_yearly_123", response.get().getPaymentLinkId());

                ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);
                verify(paymentLinkClient).create(requestCaptor.capture());
                JSONObject request = requestCaptor.getValue();
                assertEquals(9999L, request.getLong("amount"));
                assertEquals("Payment for YEARLY plan", request.getString("description"));
            }
        }

        @Test
        @DisplayName("returns empty when Razorpay API creation fails")
        void createPaymentLink_apiFailure_returnsEmpty() throws Exception {
            // Given
            User user = buildUser();
            when(userService.getUserByEmail("alice@example.com")).thenReturn(user);

            PaymentLinkClient paymentLinkClient = mock(PaymentLinkClient.class);

            try (MockedConstruction<RazorpayClient> ignored = mockConstruction(RazorpayClient.class, (client, context) -> {
                client.paymentLink = paymentLinkClient;
                when(paymentLinkClient.create(any(JSONObject.class))).thenThrow(new RuntimeException("razorpay down"));
            })) {
                // When
                Optional<PaymentLinkResponse> response = paymentService.createPaymentLink("alice@example.com", PlanType.MONTHLY);

                // Then
                assertFalse(response.isPresent());
            }
        }
    }

    @Nested
    @DisplayName("Payment completion")
    class PaymentCompletionTests {

        @Test
        @DisplayName("updates subscription after a valid signature and active session")
        void completePayment_success_updatesSubscription() throws Exception {
            // Given
            Subscription updated = new Subscription();
            updated.setSubscriptionId(10L);
            updated.setPlanType(PlanType.YEARLY);

            when(subscriptionService.updateSubscription(1L, PlanType.YEARLY)).thenReturn(updated);

            String signature = paymentService.generateSignature("order_123", "pay_123");

            // When
            Subscription result = paymentService.completePayment(
                    1L,
                    PlanType.YEARLY,
                    "order_123",
                    "pay_123",
                    signature,
                    LocalDateTime.now().minusMinutes(2)
            );

            // Then
            assertEquals(PlanType.YEARLY, result.getPlanType());
            verify(subscriptionService).updateSubscription(1L, PlanType.YEARLY);
        }

        @Test
        @DisplayName("rejects an invalid Razorpay payment signature")
        void completePayment_invalidSignature_throws() {
            // When / Then
            assertThrows(SecurityException.class, () -> paymentService.completePayment(
                    1L,
                    PlanType.MONTHLY,
                    "order_123",
                    "pay_123",
                    "bad-signature",
                    LocalDateTime.now().minusMinutes(2)
            ));
        }

        @Test
        @DisplayName("rejects expired payment sessions before updating subscription")
        void completePayment_expiredSession_throws() throws Exception {
            // Given
            String signature = paymentService.generateSignature("order_123", "pay_123");

            // When / Then
            assertThrows(IllegalStateException.class, () -> paymentService.completePayment(
                    1L,
                    PlanType.MONTHLY,
                    "order_123",
                    "pay_123",
                    signature,
                    LocalDateTime.now().minusMinutes(20)
            ));
        }
    }

    private User buildUser() {
        User user = new User();
        user.setUserId(1L);
        user.setFullName("Alice Example");
        user.setEmail("alice@example.com");
        return user;
    }
}