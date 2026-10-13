package com.yaseen.wattwise.dto;

import java.math.BigDecimal;

public record ApplianceCalculationResponse(
        String name,
        BigDecimal energyKwh,
        BigDecimal costGbp
) {
}
