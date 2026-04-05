package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookingCreateRequest {

    @NotBlank(message = "Vehicle ID is required")
    private String vehicleId;

    @NotBlank(message = "startDate is required")
    @JsonAlias({"pickupDate"})
    private String startDate;

    @NotBlank(message = "endDate is required")
    @JsonAlias({"returnDate"})
    private String endDate;

    // Optional compatibility fields used by newer frontend payloads.
    private String pickupDateTime;
    private String returnDateTime;

    // Optional: 25% advance amount pre-calculated by frontend; stored as advanceAmount if > 0.
    private BigDecimal estimatedAdvanceAmount;

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate == null ? null : startDate.toString();
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate == null ? null : startDate.toString();
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate == null ? null : endDate.toString();
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate == null ? null : endDate.toString();
    }
}

