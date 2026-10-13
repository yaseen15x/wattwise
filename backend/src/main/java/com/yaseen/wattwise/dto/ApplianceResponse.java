package com.yaseen.wattwise.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplianceResponse(
        UUID id,
        String name,
        BigDecimal powerWatts,
        Instant createdAt
) {
}
