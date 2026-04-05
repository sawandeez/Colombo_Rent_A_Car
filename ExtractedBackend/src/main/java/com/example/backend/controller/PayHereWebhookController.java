package com.example.backend.controller;

import com.example.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/payments/payhere", "/api/payments/payhere"})
@RequiredArgsConstructor
public class PayHereWebhookController {

    private final PaymentService paymentService;

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, String>> notify(@RequestParam Map<String, String> payload) {
        paymentService.handlePayHereNotify(payload);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}

