package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingRejectRequest {

    @NotBlank(message = "reason is required")
    private String reason = "Rejected by admin";

    public void setReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return;
        }
        this.reason = reason;
    }
}

