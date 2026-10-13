package com.yaseen.wattwise.model;

import java.math.BigDecimal;

public record TariffPeriod(
        int startMinute,
        int endMinute,
        BigDecimal rateGbpPerKwh
) {

    public TariffPeriod {
        if (startMinute < 0 || startMinute >= 1440) {
            throw new IllegalArgumentException(
                    "Tariff start must be between 0 and 1439"
            );
        }

        if (endMinute <= startMinute || endMinute > 1440) {
            throw new IllegalArgumentException(
                    "Tariff end must be after its start and at most 1440"
            );
        }

        if (rateGbpPerKwh == null || rateGbpPerKwh.signum() < 0) {
            throw new IllegalArgumentException(
                    "Tariff rate must be provided and cannot be negative"
            );
        }
    }
}
