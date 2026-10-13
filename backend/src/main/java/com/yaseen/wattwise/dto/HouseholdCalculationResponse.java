package com.yaseen.wattwise.dto;

import java.math.BigDecimal;
import java.util.List;

public record HouseholdCalculationResponse(
        List<ApplianceCalculationResponse> appliances,
        BigDecimal totalEnergyKwh,
        BigDecimal totalCostGbp
) {
}
