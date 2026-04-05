package com.example.backend.service.gateway;

import com.example.backend.model.Booking;
import com.example.backend.model.PaymentTransaction;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentInitiationResult initiate(Booking booking, PaymentTransaction transaction, ReturnUrls returnUrls) {
        String redirectUrl = returnUrls.getMockCheckoutUrl() + "?orderId=" + transaction.getOrderId();
        return PaymentInitiationResult.builder()
                .gateway(com.example.backend.model.PaymentGateway.MOCK)
                .redirectUrl(redirectUrl)
                .build();
    }
}

