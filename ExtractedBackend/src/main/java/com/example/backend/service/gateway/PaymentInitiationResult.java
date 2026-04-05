package com.example.backend.service.gateway;

import com.example.backend.model.PaymentGateway;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class PaymentInitiationResult {
    PaymentGateway gateway;
    String payhereUrl;
    String redirectUrl;
    Map<String, String> fields;
}

