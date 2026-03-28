package com.example.backend.dto;

import com.example.backend.model.UserRole;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String name;
    @NotBlank
    @JsonAlias({"phoneNumber"})
    private String phone;

    private Integer age;

    @NotBlank
    @JsonAlias({"districtName"})
    private String district; // Must be validated for "Colombo" in business logic or custom validator

    @NotBlank
    @JsonAlias({"cityName"})
    private String city;

    private String address;

    // Optional: role can be set here or default to CUSTOMER in service
    private UserRole role;

    public void setAddress(String address) {
        this.address = address;
        if ((this.city == null || this.city.isBlank()) && address != null && !address.isBlank()) {
            this.city = address.trim();
        }
    }
}
