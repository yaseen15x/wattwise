package com.yaseen.wattwise.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TariffPeriodRequest(
        @NotNull @Min(0) @Max(1439)
        Integer startMinute,

        @NotNull @Min(1) @Max(1440)
        Integer endMinute,

        @NotNull @DecimalMin("0")
        BigDecimal rateGbpPerKwh
) {
}
