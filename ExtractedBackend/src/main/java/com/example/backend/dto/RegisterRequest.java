package com.example.backend.dto;

import com.example.backend.model.UserRole;
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
    private String phone;
    @NotBlank
    private String district; // Must be validated for "Colombo" in business logic or custom validator
    @NotBlank
    private String city;

    // Optional: role can be set here or default to CUSTOMER in service
    private UserRole role;
}
