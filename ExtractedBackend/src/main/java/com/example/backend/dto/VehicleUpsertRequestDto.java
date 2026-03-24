package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VehicleUpsertRequestDto {
    private String name;

    private String thumbnailUrl;
    private String vehicleTypeId;

    private String make;
    private String model;

    @Min(value = 1900, message = "year must be at least 1900")
    private Integer year;

    private String licensePlate;
    private String type;
    private String description;

    private BigDecimal rentalPrice;
    private BigDecimal rentalPricePerDay;
    private List<String> imageUrls;

    @JsonProperty("isAvailable")
    private Boolean isAvailable;

    @JsonProperty("isUnderMaintenance")
    private Boolean isUnderMaintenance;

    @JsonProperty("isAdminHeld")
    private Boolean isAdminHeld;

    // Accepted for backward compatibility, but server derives status from flags.
    private String availabilityStatus;
}


