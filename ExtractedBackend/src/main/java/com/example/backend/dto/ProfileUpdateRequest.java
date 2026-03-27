package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "phone is required")
    @JsonAlias({"phoneNumber"})
    private String phone;

    @NotBlank(message = "district is required")
    @JsonAlias({"districtName"})
    private String district;

    @NotBlank(message = "city is required")
    @JsonAlias({"cityName"})
    private String city;

    private String address;

    public void setAddress(String address) {
        this.address = address;
        if ((this.city == null || this.city.isBlank()) && address != null && !address.isBlank()) {
            this.city = address.trim();
        }
    }
}

