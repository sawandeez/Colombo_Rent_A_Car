package com.example.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SetAdvanceAmountRequest {

    @NotNull(message = "advanceAmount is required")
    @DecimalMin(value = "0", message = "advanceAmount must be >= 0")
    private BigDecimal advanceAmount;

    private String advanceCurrency;
}

