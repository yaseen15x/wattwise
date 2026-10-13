package com.yaseen.wattwise.service;

import com.yaseen.wattwise.model.OptimisationWindow;
import com.yaseen.wattwise.model.ScheduledAppliance;
import com.yaseen.wattwise.model.TariffPeriod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScheduleOptimiserTest {

    private final TimeBasedEnergyCalculator calculator =
            new TimeBasedEnergyCalculator();

    private final ScheduleOptimiser optimiser =
            new ScheduleOptimiser(calculator);

    @Test
    void movesApplianceIntoCheaperPeriod() {
        ScheduledAppliance original = new ScheduledAppliance(
                "Heater", new BigDecimal("2000"), 360, 60
        );

        OptimisationWindow window = new OptimisationWindow(360, 600);

        List<TariffPeriod> tariffs = List.of(
                new TariffPeriod(0, 420, new BigDecimal("0.30")),
                new TariffPeriod(420, 1440, new BigDecimal("0.10"))
        );

        ScheduledAppliance result =
                optimiser.optimise(original, window, tariffs);

        assertEquals(420, result.startMinute());
        assertEquals(60, result.durationMinutes());
        assertEquals("Heater", result.name());
        assertEquals(original.powerWatts(), result.powerWatts());

        assertEquals(
                new BigDecimal("0.20"),
                calculator.calculate(result, tariffs).costGbp()
        );

        // The original object has not changed.
        assertEquals(360, original.startMinute());
    }

    @Test
    void choosesEarliestStartWhenCostsAreEqual() {
        ScheduledAppliance original = new ScheduledAppliance(
                "Heater", new BigDecimal("2000"), 480, 60
        );

        OptimisationWindow window = new OptimisationWindow(360, 600);

        List<TariffPeriod> tariffs = List.of(
                new TariffPeriod(0, 1440, new BigDecimal("0.25"))
        );

        ScheduledAppliance result =
                optimiser.optimise(original, window, tariffs);

        assertEquals(360, result.startMinute());
    }

    @Test
    void rejectsAnApplianceThatCannotMeetTheDeadline() {
        ScheduledAppliance original = new ScheduledAppliance(
                "Heater", new BigDecimal("2000"), 360, 120
        );

        OptimisationWindow window = new OptimisationWindow(360, 420);

        List<TariffPeriod> tariffs = List.of(
                new TariffPeriod(0, 1440, new BigDecimal("0.25"))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> optimiser.optimise(original, window, tariffs)
        );
    }

    @Test
    void comparesCostsBeforeRoundingToPennies() {
        ScheduledAppliance original = new ScheduledAppliance(
                "Lamp", new BigDecimal("10"), 0, 15
        );

        OptimisationWindow window = new OptimisationWindow(0, 30);

        List<TariffPeriod> tariffs = List.of(
                new TariffPeriod(0, 15, new BigDecimal("0.30")),
                new TariffPeriod(15, 1440, new BigDecimal("0.10"))
        );

        ScheduledAppliance result =
                optimiser.optimise(original, window, tariffs);

        // Both displayed costs are £0.00, but minute 15 is cheaper.
        assertEquals(
                new BigDecimal("0.00"),
                calculator.calculate(original, tariffs).costGbp()
        );
        assertEquals(
                new BigDecimal("0.00"),
                calculator.calculate(result, tariffs).costGbp()
        );
        assertEquals(15, result.startMinute());
    }

    @Test
    void keepsOriginalStartWhenItIsCheaperThanGridCandidates() {
        ScheduledAppliance original = new ScheduledAppliance(
                "Heater", new BigDecimal("2000"), 7, 5
        );

        OptimisationWindow window = new OptimisationWindow(0, 30);

        List<TariffPeriod> tariffs = List.of(
                new TariffPeriod(0, 7, new BigDecimal("0.30")),
                new TariffPeriod(7, 12, new BigDecimal("0.10")),
                new TariffPeriod(12, 1440, new BigDecimal("0.30"))
        );

        ScheduledAppliance result =
                optimiser.optimise(original, window, tariffs);

        assertEquals(7, result.startMinute());
    }
}
