package com.yaseen.wattwise.service;

import com.yaseen.wattwise.dto.ApplianceUsageRequest;
import com.yaseen.wattwise.dto.HouseholdCalculationRequest;
import com.yaseen.wattwise.dto.HouseholdCalculationResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HouseholdCalculatorTest {

    private final HouseholdCalculator calculator =
            new HouseholdCalculator(new EnergyCalculator());

    @Test
    void calculatesApplianceBreakdownAndHouseholdTotals() {
        HouseholdCalculationRequest request =
                new HouseholdCalculationRequest(
                        List.of(
                                new ApplianceUsageRequest(
                                        "Heater",
                                        new BigDecimal("2000"),
                                        new BigDecimal("3")
                                ),
                                new ApplianceUsageRequest(
                                        "PC",
                                        new BigDecimal("300"),
                                        new BigDecimal("5")
                                )
                        ),
                        new BigDecimal("0.25")
                );

        HouseholdCalculationResponse result =
                calculator.calculate(request);

        assertEquals(2, result.appliances().size());

        assertEquals("Heater", result.appliances().get(0).name());
        assertEquals(
                new BigDecimal("1.50"),
                result.appliances().get(0).costGbp()
        );

        assertEquals("PC", result.appliances().get(1).name());
        assertEquals(
                new BigDecimal("0.38"),
                result.appliances().get(1).costGbp()
        );

        assertEquals(
                0,
                new BigDecimal("7.5").compareTo(result.totalEnergyKwh())
        );
        assertEquals(new BigDecimal("1.88"), result.totalCostGbp());
    }

    @Test
    void roundsHouseholdCostOnlyAfterCombiningEnergy() {
        HouseholdCalculationRequest request =
                new HouseholdCalculationRequest(
                        List.of(
                                new ApplianceUsageRequest(
                                        "Lamp A",
                                        new BigDecimal("10"),
                                        BigDecimal.ONE
                                ),
                                new ApplianceUsageRequest(
                                        "Lamp B",
                                        new BigDecimal("10"),
                                        BigDecimal.ONE
                                )
                        ),
                        new BigDecimal("0.30")
                );

        HouseholdCalculationResponse result =
                calculator.calculate(request);

        assertEquals(
                new BigDecimal("0.00"),
                result.appliances().get(0).costGbp()
        );
        assertEquals(
                new BigDecimal("0.00"),
                result.appliances().get(1).costGbp()
        );
        assertEquals(new BigDecimal("0.01"), result.totalCostGbp());
    }
}
