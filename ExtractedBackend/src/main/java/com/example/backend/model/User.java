package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String name;

    private String phone;

    private Integer age;

    private String address;

    // Core requirement: Location restriction (Colombo)
    private String district;

    private String city;

    private UserRole role;

    // For document verification status (relevant for Customers)
    private boolean documentsVerified = false;

    private String nicFrontPath;
    private String drivingLicensePath;
    private LocalDateTime documentsUpdatedAt;
    private LocalDateTime documentsVerifiedAt;
}
