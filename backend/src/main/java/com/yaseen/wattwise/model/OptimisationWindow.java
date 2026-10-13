package com.yaseen.wattwise.model;

public record OptimisationWindow(
        int earliestStartMinute,
        int latestFinishMinute
) {

    public OptimisationWindow {
        if (earliestStartMinute < 0 || earliestStartMinute >= 1440) {
            throw new IllegalArgumentException(
                    "Earliest start must be between 0 and 1439"
            );
        }

        if (latestFinishMinute <= earliestStartMinute
                || latestFinishMinute > 1440) {
            throw new IllegalArgumentException(
                    "Latest finish must be after earliest start "
                            + "and at most 1440"
            );
        }
    }

    public boolean canFit(int durationMinutes) {
        return durationMinutes > 0
                && durationMinutes <= latestFinishMinute - earliestStartMinute;
    }
}
