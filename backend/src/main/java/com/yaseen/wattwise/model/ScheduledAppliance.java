package com.yaseen.wattwise.model;

import java.math.BigDecimal;

public record ScheduledAppliance(
        String name,
        BigDecimal powerWatts,
        int startMinute,
        int durationMinutes
) {

    public ScheduledAppliance {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Appliance name must not be blank"
            );
        }

        if (powerWatts == null || powerWatts.signum() < 0) {
            throw new IllegalArgumentException(
                    "Power must be provided and cannot be negative"
            );
        }

        if (startMinute < 0 || startMinute >= 1440) {
            throw new IllegalArgumentException(
                    "Start time must be between 0 and 1439"
            );
        }

        if (durationMinutes <= 0 || durationMinutes > 1440) {
            throw new IllegalArgumentException(
                    "Duration must be between 1 and 1440 minutes"
            );
        }
    }
}