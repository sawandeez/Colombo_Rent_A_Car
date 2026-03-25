package com.example.backend.dto;

import com.example.backend.model.BookingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingResponse {
    private String id;
    private String userId;
    private String vehicleId;
    private VehicleSummaryDto vehicle;
    private LocalDateTime bookingTime;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BookingStatus status;
    private BigDecimal advanceAmount;
    private boolean advancePaid;
    private String rejectionReason;
    private String nicFrontDocumentId;
    private String drivingLicenseDocumentId;
}
