package com.yaseen.wattwise.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ApplianceUsageRequest(
        @NotBlank
        String name,

        @NotNull @DecimalMin("0")
        BigDecimal powerWatts,

        @NotNull @DecimalMin("0")
        BigDecimal durationHours
) {
}