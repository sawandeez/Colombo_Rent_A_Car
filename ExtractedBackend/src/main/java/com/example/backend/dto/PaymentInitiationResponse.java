package com.example.backend.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class PaymentInitiationResponse {
    String bookingId;
    String gateway;
    String orderId;
    String payhereUrl;
    String redirectUrl;
    Map<String, String> fields;
}
