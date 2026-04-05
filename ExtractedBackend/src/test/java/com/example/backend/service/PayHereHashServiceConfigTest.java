package com.example.backend.service;

import com.example.backend.config.PayHereProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayHereHashServiceConfigTest {

    @Test
    void usesSandboxConfigWhenEnvironmentIsSandbox() {
        PayHereProperties properties = buildProperties();
        properties.setEnvironment("sandbox");

        PayHereHashService service = new PayHereHashService(properties);

        assertEquals("SANDBOX_ID", service.getMerchantId());
        assertEquals("https://sandbox.payhere.lk/pay/checkout", service.getCheckoutUrl());
        assertEquals("sandbox", service.getEnvironment());
    }

    @Test
    void usesLiveConfigWhenEnvironmentIsLive() {
        PayHereProperties properties = buildProperties();
        properties.setEnvironment("live");

        PayHereHashService service = new PayHereHashService(properties);

        assertEquals("LIVE_ID", service.getMerchantId());
        assertEquals("https://www.payhere.lk/pay/checkout", service.getCheckoutUrl());
        assertEquals("live", service.getEnvironment());
    }

    private PayHereProperties buildProperties() {
        PayHereProperties properties = new PayHereProperties();

        properties.getSandbox().setMerchantId("SANDBOX_ID");
        properties.getSandbox().setMerchantSecret("SANDBOX_SECRET");
        properties.getSandbox().setCheckoutUrl("https://sandbox.payhere.lk/pay/checkout");

        properties.getLive().setMerchantId("LIVE_ID");
        properties.getLive().setMerchantSecret("LIVE_SECRET");
        properties.getLive().setCheckoutUrl("https://www.payhere.lk/pay/checkout");

        return properties;
    }
}

