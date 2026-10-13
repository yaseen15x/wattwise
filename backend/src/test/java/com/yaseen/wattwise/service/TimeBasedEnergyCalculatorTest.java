package com.yaseen.wattwise.service;

import com.yaseen.wattwise.dto.EnergyCalculationResponse;
import com.yaseen.wattwise.model.ScheduledAppliance;
import com.yaseen.wattwise.model.TariffPeriod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeBasedEnergyCalculatorTest {

    private final TimeBasedEnergyCalculator calculator =
            new TimeBasedEnergyCalculator();

    @Test
    void splitsUsageAcrossTwoRates() {
        ScheduledAppliance heater = new ScheduledAppliance(
                "Heater", new BigDecimal("2000"), 1050, 60
        );

        List<TariffPeriod> tariffs = List.of(
                new TariffPeriod(0, 1080, new BigDecimal("0.20")),
                new TariffPeriod(1080, 1440, new BigDecimal("0.30"))
        );

        EnergyCalculationResponse result =
                calculator.calculate(heater, tariffs);

        assertEquals(
                0,
                new BigDecimal("2").compareTo(result.energyKwh())
        );
        assertEquals(new BigDecimal("0.50"), result.costGbp());
    }

    @Test
    void handlesUsageAcrossMidnight() {
        ScheduledAppliance heater = new ScheduledAppliance(
                "Heater", new BigDecimal("2000"), 1410, 60
        );

        List<TariffPeriod> tariffs = List.of(
                new TariffPeriod(0, 420, new BigDecimal("0.10")),
                new TariffPeriod(420, 1440, new BigDecimal("0.30"))
        );

        EnergyCalculationResponse result =
                calculator.calculate(heater, tariffs);

        assertEquals(
                0,
                new BigDecimal("2").compareTo(result.energyKwh())
        );
        assertEquals(new BigDecimal("0.40"), result.costGbp());
    }

    @Test
    void rejectsGapsInTariffCoverage() {
        ScheduledAppliance heater = new ScheduledAppliance(
                "Heater", new BigDecimal("2000"), 0, 60
        );

        List<TariffPeriod> tariffs = List.of(
                new TariffPeriod(0, 420, new BigDecimal("0.10")),
                new TariffPeriod(480, 1440, new BigDecimal("0.30"))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(heater, tariffs)
        );
    }

    @Test
    void rejectsOverlappingTariffPeriods() {
        ScheduledAppliance heater = new ScheduledAppliance(
                "Heater", new BigDecimal("2000"), 0, 60
        );

        List<TariffPeriod> tariffs = List.of(
                new TariffPeriod(0, 480, new BigDecimal("0.10")),
                new TariffPeriod(420, 1440, new BigDecimal("0.30"))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(heater, tariffs)
        );
    }
}