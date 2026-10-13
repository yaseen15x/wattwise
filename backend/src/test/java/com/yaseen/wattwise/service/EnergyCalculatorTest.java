package com.yaseen.wattwise.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnergyCalculatorTest {

    private final EnergyCalculator calculator = new EnergyCalculator();

    @Test
    void heaterUsesSixKwhAndCostsOnePoundFifty() {
        BigDecimal energy = calculator.calculateEnergyKwh(
                new BigDecimal("2000"),
                new BigDecimal("3")
        );

        BigDecimal cost = calculator.calculateCostGbp(
                energy,
                new BigDecimal("0.25")
        );

        assertEquals(0, new BigDecimal("6").compareTo(energy));
        assertEquals(new BigDecimal("1.50"), cost);
    }

    @Test
    void supportsPartialHoursAndRoundsCostToNearestPenny() {
        BigDecimal energy = calculator.calculateEnergyKwh(
                new BigDecimal("1500"),
                new BigDecimal("0.5")
        );

        BigDecimal cost = calculator.calculateCostGbp(
                energy,
                new BigDecimal("0.23")
        );

        assertEquals(0, new BigDecimal("0.75").compareTo(energy));
        assertEquals(new BigDecimal("0.17"), cost);
    }

    @Test
    void rejectsNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateEnergyKwh(
                        new BigDecimal("2000"),
                        new BigDecimal("-1")
                )
        );
    }
}