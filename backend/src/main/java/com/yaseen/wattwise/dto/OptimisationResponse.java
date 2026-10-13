package com.yaseen.wattwise.dto;

import java.math.BigDecimal;

public record OptimisationResponse(
        String name,
        int originalStartMinute,
        int recommendedStartMinute,
        int durationMinutes,
        boolean originalMeetsWindow,
        BigDecimal originalCostGbp,
        BigDecimal recommendedCostGbp,
        BigDecimal estimatedSavingsGbp
) {
}
