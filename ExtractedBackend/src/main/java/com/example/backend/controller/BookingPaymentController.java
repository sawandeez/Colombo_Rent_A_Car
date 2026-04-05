package com.example.backend.controller;

import com.example.backend.dto.PaymentInitiationResponse;
import com.example.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/bookings", "/api/bookings"})
@RequiredArgsConstructor
public class BookingPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{bookingId}/payments/initiate")
    public ResponseEntity<PaymentInitiationResponse> initiate(@PathVariable String bookingId) {
        return ResponseEntity.ok(paymentService.initiatePayHerePayment(bookingId));
    }
}

