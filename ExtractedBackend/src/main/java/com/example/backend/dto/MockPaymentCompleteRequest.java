package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MockPaymentCompleteRequest {
    @NotBlank
    private String orderId;

    @NotBlank
    private String status;
}

