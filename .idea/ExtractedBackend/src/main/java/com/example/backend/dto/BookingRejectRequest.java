package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingRejectRequest {

    @NotBlank(message = "reason is required")
    private String reason;
}

