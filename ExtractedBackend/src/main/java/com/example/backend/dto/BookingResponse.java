package com.example.backend.dto;

import com.example.backend.model.BookingStatus;
import com.example.backend.model.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class BookingResponse {
    private String id;
    private String userId;
    private AdminUserResponse user;
    private String vehicleId;
    private VehicleSummaryDto vehicle;
    private String vehicleName;
    private LocalDateTime bookingTime;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BookingStatus status;
    private BigDecimal advanceAmount;
    private String advanceCurrency;
    private BigDecimal totalPrice;
    private boolean advancePaid;
    private PaymentStatus paymentStatus;
    private Instant paymentDate;
    private String rejectionReason;
    private String nicFrontDocumentId;
    private String drivingLicenseDocumentId;

    public String getBookingId() {
        return id;
    }

    public LocalDateTime getPickupDateTime() {
        return startDate;
    }

    public LocalDateTime getReturnDateTime() {
        return endDate;
    }

    public LocalDateTime getCreatedAt() {
        return bookingTime;
    }

    public BigDecimal getTotalAmount() {
        return totalPrice;
    }
}
