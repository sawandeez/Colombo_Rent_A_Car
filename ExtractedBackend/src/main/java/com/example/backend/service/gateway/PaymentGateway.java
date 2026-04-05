package com.example.backend.service.gateway;

import com.example.backend.model.Booking;
import com.example.backend.model.PaymentTransaction;
import com.example.backend.service.gateway.PaymentInitiationResult;
import com.example.backend.service.gateway.ReturnUrls;

public interface PaymentGateway {
    PaymentInitiationResult initiate(Booking booking, PaymentTransaction transaction, ReturnUrls returnUrls);
}
