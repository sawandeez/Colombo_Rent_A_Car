package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Document(collection = "bookings")
public class Booking {
    @Id
    private String id;

    private String userId;
    private String vehicleId;

    private LocalDateTime bookingTime;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private BookingStatus status;

    private BigDecimal advanceAmount;
    private String advanceCurrency;
    private boolean advancePaid = false;
    private PaymentStatus paymentStatus;
    private Instant paymentDate;

    private String rejectionReason;

    private String nicFrontDocumentId;
    private String drivingLicenseDocumentId;
}
