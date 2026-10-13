package com.yaseen.wattwise.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OptimisationRequest(
        @NotNull @Valid
        TimeBasedCalculationRequest schedule,

        @NotNull @Min(0) @Max(1439)
        Integer earliestStartMinute,

        @NotNull @Min(1) @Max(1440)
        Integer latestFinishMinute
) {
}

