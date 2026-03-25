package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingCreateRequest {

    @NotBlank(message = "Vehicle ID is required")
    private String vehicleId;

    @NotNull(message = "pickupDate is required")
    @Future(message = "pickupDate must be in the future")
    @JsonAlias("startDate")
    private LocalDateTime pickupDate;

    @NotNull(message = "returnDate is required")
    @Future(message = "returnDate must be in the future")
    @JsonAlias("endDate")
    private LocalDateTime returnDate;
}

