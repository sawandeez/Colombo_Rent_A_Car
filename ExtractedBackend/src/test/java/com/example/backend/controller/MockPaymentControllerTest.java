package com.example.backend.controller;

import com.example.backend.security.JwtService;
import com.example.backend.security.SecurityConfiguration;
import com.example.backend.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MockPaymentController.class)
@Import(SecurityConfiguration.class)
class MockPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Test
    void checkoutRendersMockHtml() throws Exception {
        when(paymentService.getMockCheckoutContext("ORD-123")).thenReturn(Map.of(
                "orderId", "ORD-123",
                "bookingId", "b-1",
                "completeUrl", "/api/v1/payments/mock/complete",
                "gateway", "MOCK"
        ));

        mockMvc.perform(get("/api/v1/payments/mock/checkout").param("orderId", "ORD-123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"));
    }

    @Test
    void completeDelegatesToService() throws Exception {
        when(paymentService.completeMockPayment(eq("ORD-123"), eq("SUCCESS"))).thenReturn(Map.of(
                "status", "ok",
                "paymentStatus", "SUCCESS",
                "redirectUrl", "http://localhost:5173/payment/success?bookingId=b-1"
        ));

        mockMvc.perform(post("/api/v1/payments/mock/complete")
                        .contentType("application/json")
                        .content("{\"orderId\":\"ORD-123\",\"status\":\"SUCCESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        verify(paymentService).completeMockPayment("ORD-123", "SUCCESS");
    }
}

