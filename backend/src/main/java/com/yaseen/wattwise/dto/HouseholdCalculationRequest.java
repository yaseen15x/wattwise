package com.yaseen.wattwise.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record HouseholdCalculationRequest(
        @NotEmpty
        @Size(max = 100)
        List<@NotNull @Valid ApplianceUsageRequest> appliances,

        @NotNull @DecimalMin("0")
        BigDecimal rateGbpPerKwh
) {
}
