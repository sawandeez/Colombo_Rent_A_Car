package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.util.List;

@Data
@Document(collection = "vehicles")
public class Vehicle {
    @Id
    private String id;

    private String name;
    private String thumbnailUrl;
    private String vehicleTypeId;
    private BigDecimal rentalPrice;
    private VehicleAvailabilityStatus availabilityStatus;

    // Extended vehicle fields
    private String make;
    private String model;
    private int year;
    private String licensePlate;
    private String type; // e.g., Sedan, SUV
    private String description;

    private BigDecimal rentalPricePerDay;

    // Images
    private List<String> imageUrls;

    // Availability
    private boolean isAvailable = true;
    private boolean isUnderMaintenance = false;
    private boolean isAdminHeld = false;

    public void setAvailable(boolean available) {
        this.isAvailable = available;
        syncStatusFromFlags();
    }

    public void setUnderMaintenance(boolean underMaintenance) {
        this.isUnderMaintenance = underMaintenance;
        syncStatusFromFlags();
    }

    public void setAdminHeld(boolean adminHeld) {
        this.isAdminHeld = adminHeld;
        syncStatusFromFlags();
    }

    public void syncStatusFromFlags() {
        this.availabilityStatus = VehicleAvailabilityStatus.fromFlags(isAvailable, isUnderMaintenance, isAdminHeld);
    }
}
