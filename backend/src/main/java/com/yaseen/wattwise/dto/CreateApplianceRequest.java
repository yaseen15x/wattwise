package com.yaseen.wattwise.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateApplianceRequest(
        @NotBlank @Size(max = 100)
        String name,

        @NotNull
        @DecimalMin("0")
        @Digits(integer = 9, fraction = 3)
        BigDecimal powerWatts
) {
}
