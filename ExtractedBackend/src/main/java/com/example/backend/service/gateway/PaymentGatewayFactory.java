package com.example.backend.service.gateway;

import com.example.backend.model.PaymentGateway;
import com.example.backend.service.PayHereHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayFactory {

    private final PayHerePaymentGateway payHerePaymentGateway;
    private final MockPaymentGateway mockPaymentGateway;
    private final PayHereHashService payHereHashService;

    @Value("${app.payment.gateway:MOCK}")
    private String configuredGateway;

    public PaymentGateway resolveGateway() {
        String mode = configuredGateway == null ? "MOCK" : configuredGateway.trim().toUpperCase(Locale.ROOT);
        if ("PAYHERE".equals(mode)) {
            if (payHereHashService.hasCredentialsConfigured()) {
                return PaymentGateway.PAYHERE;
            }
            log.warn("PAYMENT_GATEWAY - PAYHERE requested but merchant credentials are missing for environment {}; falling back to MOCK",
                    payHereHashService.getEnvironment());
        }
        return PaymentGateway.MOCK;
    }

    public com.example.backend.service.gateway.PaymentGateway gatewayBean(PaymentGateway gateway) {
        if (gateway == PaymentGateway.PAYHERE) {
            return payHerePaymentGateway;
        }
        return mockPaymentGateway;
    }
}

