package com.yaseen.wattwise.dto;

import java.math.BigDecimal;

public record EnergyCalculationResponse(
        BigDecimal energyKwh,
        BigDecimal costGbp
) {
}
