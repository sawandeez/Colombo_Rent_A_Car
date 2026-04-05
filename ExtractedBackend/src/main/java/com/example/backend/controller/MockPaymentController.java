package com.example.backend.controller;

import com.example.backend.dto.MockPaymentCompleteRequest;
import com.example.backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/payments/mock", "/api/payments/mock"})
@RequiredArgsConstructor
public class MockPaymentController {

    private final PaymentService paymentService;

    @GetMapping(value = "/checkout", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> checkout(@RequestParam String orderId) {
        Map<String, String> context = paymentService.getMockCheckoutContext(orderId);
        String page = """
                <!doctype html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <title>Mock Payment Checkout</title>
                </head>
                <body style="font-family: Arial, sans-serif; margin: 2rem;">
                  <h2>Mock Payment Gateway</h2>
                  <p>Order ID: <strong>%s</strong></p>
                  <p>Booking ID: <strong>%s</strong></p>
                  <button onclick="complete('SUCCESS')" style="padding: 10px 14px; margin-right: 8px;">Simulate Success</button>
                  <button onclick="complete('FAILED')" style="padding: 10px 14px;">Simulate Failure</button>
                  <script>
                    async function complete(status) {
                      const response = await fetch('%s', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ orderId: '%s', status: status })
                      });
                      const data = await response.json();
                      if (data.redirectUrl) {
                        window.location.href = data.redirectUrl;
                      }
                    }
                  </script>
                </body>
                </html>
                """.formatted(
                context.get("orderId"),
                context.get("bookingId"),
                context.get("completeUrl"),
                context.get("orderId"));

        return ResponseEntity.ok(page);
    }

    @PostMapping("/complete")
    public ResponseEntity<Map<String, String>> complete(@Valid @RequestBody MockPaymentCompleteRequest request) {
        return ResponseEntity.ok(paymentService.completeMockPayment(request.getOrderId(), request.getStatus()));
    }
}
