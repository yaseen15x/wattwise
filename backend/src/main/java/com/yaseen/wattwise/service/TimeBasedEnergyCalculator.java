package com.yaseen.wattwise.service;

import com.yaseen.wattwise.dto.EnergyCalculationResponse;
import com.yaseen.wattwise.model.ScheduledAppliance;
import com.yaseen.wattwise.model.TariffPeriod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TimeBasedEnergyCalculator {

    private static final int MINUTES_PER_DAY = 1440;

    // Convert watt-minutes to kilowatt-hours: divide by 1000 × 60.
    private static final BigDecimal WATT_MINUTES_PER_KWH =
            new BigDecimal("60000");

    public EnergyCalculationResponse calculate(
            ScheduledAppliance appliance,
            List<TariffPeriod> tariffs) {

        if (appliance == null) {
            throw new IllegalArgumentException("Appliance is required");
        }

        BigDecimal[] rates = buildMinuteRates(tariffs);
        BigDecimal weightedTotal = BigDecimal.ZERO;

        for (int offset = 0; offset < appliance.durationMinutes(); offset++) {
            int minuteOfDay =
                    (appliance.startMinute() + offset) % MINUTES_PER_DAY;

            BigDecimal weightedMinute =
                    appliance.powerWatts().multiply(rates[minuteOfDay]);

            weightedTotal = weightedTotal.add(weightedMinute);
        }

        BigDecimal energyKwh = appliance.powerWatts()
                .multiply(BigDecimal.valueOf(appliance.durationMinutes()))
                .divide(
                        WATT_MINUTES_PER_KWH,
                        6,
                        RoundingMode.HALF_UP
                );

        BigDecimal costGbp = weightedTotal.divide(
                WATT_MINUTES_PER_KWH,
                2,
                RoundingMode.HALF_UP
        );

        return new EnergyCalculationResponse(energyKwh, costGbp);
    }

    private BigDecimal[] buildMinuteRates(List<TariffPeriod> tariffs) {
        if (tariffs == null || tariffs.isEmpty()) {
            throw new IllegalArgumentException("Tariffs are required");
        }

        BigDecimal[] rates = new BigDecimal[MINUTES_PER_DAY];

        for (TariffPeriod period : tariffs) {
            if (period == null) {
                throw new IllegalArgumentException(
                        "Tariff periods cannot be null"
                );
            }

            for (int minute = period.startMinute();
                 minute < period.endMinute();
                 minute++) {

                if (rates[minute] != null) {
                    throw new IllegalArgumentException(
                            "Tariff periods overlap at minute " + minute
                    );
                }

                rates[minute] = period.rateGbpPerKwh();
            }
        }

        for (int minute = 0; minute < MINUTES_PER_DAY; minute++) {
            if (rates[minute] == null) {
                throw new IllegalArgumentException(
                        "Tariffs do not cover minute " + minute
                );
            }
        }

        return rates;
    }
}
