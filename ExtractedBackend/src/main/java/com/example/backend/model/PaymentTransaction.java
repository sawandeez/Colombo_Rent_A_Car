package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Document(collection = "payment_transactions")
public class PaymentTransaction {

    @Id
    private String id;

    private String bookingId;
    private PaymentGateway gateway;

    @Indexed(unique = true)
    private String orderId;

    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String gatewayPaymentId;

    private String rawNotifyPayload;
    private Instant createdAt;
    private Instant updatedAt;

    @Transient
    private User initiatedBy;
}
