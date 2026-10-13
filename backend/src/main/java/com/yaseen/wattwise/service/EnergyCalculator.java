package com.yaseen.wattwise.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class EnergyCalculator {

    public BigDecimal calculateEnergyKwh(
            BigDecimal powerWatts,
            BigDecimal durationHours) {

        requireNonNegative(powerWatts, "Power");
        requireNonNegative(durationHours, "Duration");

        return powerWatts
                .multiply(durationHours)
                .divide(new BigDecimal("1000"));
    }

    public BigDecimal calculateCostGbp(
            BigDecimal energyKwh,
            BigDecimal rateGbpPerKwh) {

        requireNonNegative(energyKwh, "Energy");
        requireNonNegative(rateGbpPerKwh, "Rate");

        return energyKwh
                .multiply(rateGbpPerKwh)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(
                    field + " must be provided and cannot be negative"
            );
        }
    }
}