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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PayHereWebhookController.class)
@Import(SecurityConfiguration.class)
class PayHereWebhookControllerTest {

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
    void notifyEndpointDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/payments/payhere/notify")
                        .contentType("application/x-www-form-urlencoded")
                        .param("order_id", "ORD-b-1-ABC12345")
                        .param("status_code", "2")
                        .param("payment_id", "PH-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        verify(paymentService).handlePayHereNotify(any());
    }
}
