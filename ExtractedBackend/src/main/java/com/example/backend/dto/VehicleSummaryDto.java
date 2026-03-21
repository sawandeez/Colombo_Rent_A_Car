package com.example.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class VehicleSummaryDto {
    private String id;
    private String name;
    private String thumbnailUrl;
    private String vehicleTypeId;
    private BigDecimal rentalPrice;
    private String availabilityStatus;

    private String make;
    private String model;
    private int year;
    private String licensePlate;
    private String type;
    private String description;
    private BigDecimal rentalPricePerDay;
    private List<String> imageUrls;
    @JsonProperty("isAvailable")
    private boolean isAvailable;
    @JsonProperty("isUnderMaintenance")
    private boolean isUnderMaintenance;
    @JsonProperty("isAdminHeld")
    private boolean isAdminHeld;

    // Constructor for DataSeeder and tests
    public VehicleSummaryDto(String name, String thumbnailUrl) {
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
    }

    // Default constructor
    public VehicleSummaryDto() {}
}
