package com.example.backend.service.gateway;

import com.example.backend.model.PaymentGateway;
import com.example.backend.service.PayHereHashService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayFactoryTest {

    @Mock
    private PayHerePaymentGateway payHerePaymentGateway;

    @Mock
    private MockPaymentGateway mockPaymentGateway;

    @Mock
    private PayHereHashService payHereHashService;

    @Test
    void usesSandboxPayHereWhenConfiguredAndCredentialsPresent() {
        PaymentGatewayFactory factory = new PaymentGatewayFactory(payHerePaymentGateway, mockPaymentGateway, payHereHashService);
        ReflectionTestUtils.setField(factory, "configuredGateway", "PAYHERE");
        when(payHereHashService.hasCredentialsConfigured()).thenReturn(true);

        assertEquals(PaymentGateway.PAYHERE, factory.resolveGateway());
    }

    @Test
    void fallsBackToMockWhenPayHereConfiguredWithoutCredentials() {
        PaymentGatewayFactory factory = new PaymentGatewayFactory(payHerePaymentGateway, mockPaymentGateway, payHereHashService);
        ReflectionTestUtils.setField(factory, "configuredGateway", "PAYHERE");
        when(payHereHashService.hasCredentialsConfigured()).thenReturn(false);
        when(payHereHashService.getEnvironment()).thenReturn("sandbox");

        assertEquals(PaymentGateway.MOCK, factory.resolveGateway());
    }

    @Test
    void usesMockWhenConfiguredMock() {
        PaymentGatewayFactory factory = new PaymentGatewayFactory(payHerePaymentGateway, mockPaymentGateway, payHereHashService);
        ReflectionTestUtils.setField(factory, "configuredGateway", "MOCK");

        assertEquals(PaymentGateway.MOCK, factory.resolveGateway());
    }
}

