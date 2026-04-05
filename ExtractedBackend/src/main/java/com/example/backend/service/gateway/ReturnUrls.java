package com.example.backend.service.gateway;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReturnUrls {
    String returnUrl;
    String cancelUrl;
    String notifyUrl;
    String mockCheckoutUrl;
}

