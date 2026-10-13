package com.yaseen.wattwise.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record TimeBasedCalculationRequest(
        @NotBlank
        String name,

        @NotNull @DecimalMin("0")
        BigDecimal powerWatts,

        @NotNull @Min(0) @Max(1439)
        Integer startMinute,

        @NotNull @Min(1) @Max(1440)
        Integer durationMinutes,

        @NotEmpty @Size(max = 1440)
        List<@NotNull @Valid TariffPeriodRequest> tariffs
) {
}