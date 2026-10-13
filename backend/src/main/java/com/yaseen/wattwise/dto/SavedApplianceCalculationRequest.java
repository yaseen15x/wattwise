package com.yaseen.wattwise.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SavedApplianceCalculationRequest(
        @NotNull @DecimalMin("0")
        BigDecimal durationHours,

        @NotNull @DecimalMin("0")
        BigDecimal rateGbpPerKwh
) {
}
