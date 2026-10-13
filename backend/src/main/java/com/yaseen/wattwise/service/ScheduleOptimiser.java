package com.yaseen.wattwise.service;

import com.yaseen.wattwise.model.OptimisationWindow;
import com.yaseen.wattwise.model.ScheduledAppliance;
import com.yaseen.wattwise.model.TariffPeriod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ScheduleOptimiser {

    private static final int STEP_MINUTES = 15;

    private final TimeBasedEnergyCalculator calculator;

    public ScheduleOptimiser(TimeBasedEnergyCalculator calculator) {
        this.calculator = calculator;
    }

    public ScheduledAppliance optimise(
            ScheduledAppliance appliance,
            OptimisationWindow window,
            List<TariffPeriod> tariffs) {

        if (appliance == null || window == null) {
            throw new IllegalArgumentException(
                    "Appliance and optimisation window are required"
            );
        }

        if (!window.canFit(appliance.durationMinutes())) {
            throw new IllegalArgumentException(
                    "Appliance cannot finish within the allowed window"
            );
        }

        int latestStart =
                window.latestFinishMinute() - appliance.durationMinutes();

        ScheduledAppliance bestSchedule = new ScheduledAppliance(
                appliance.name(),
                appliance.powerWatts(),
                window.earliestStartMinute(),
                appliance.durationMinutes()
        );

        BigDecimal bestWeight =
                calculator.calculateCostWeight(bestSchedule, tariffs);

        for (int start = window.earliestStartMinute() + STEP_MINUTES;
             start <= latestStart;
             start += STEP_MINUTES) {

            ScheduledAppliance candidate = new ScheduledAppliance(
                    appliance.name(),
                    appliance.powerWatts(),
                    start,
                    appliance.durationMinutes()
            );

            BigDecimal candidateWeight =
                    calculator.calculateCostWeight(candidate, tariffs);

            if (candidateWeight.compareTo(bestWeight) < 0) {
                bestSchedule = candidate;
                bestWeight = candidateWeight;
            }
        }

        // Also consider the original start if it fits the allowed window.
        if (appliance.startMinute() >= window.earliestStartMinute()
                && appliance.startMinute() <= latestStart) {

            BigDecimal originalWeight =
                    calculator.calculateCostWeight(appliance, tariffs);

            int comparison = originalWeight.compareTo(bestWeight);

            if (comparison < 0
                    || (comparison == 0
                    && appliance.startMinute() < bestSchedule.startMinute())) {
                bestSchedule = appliance;
            }
        }

        return bestSchedule;
    }
}
