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

    private String make;
    private String model;
    private int year;
    private String type; // e.g., Sedan, SUV
    private String description;

    private BigDecimal rentalPricePerDay;

    // Images
    private List<String> imageUrls;

    // Availability
    private boolean isAvailable = true;
    private boolean isUnderMaintenance = false;
    private boolean isAdminHeld = false;
}