package com.yaseen.wattwise.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appliances")
public class Appliance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(
            name = "power_watts",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal powerWatts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Appliance() {
    }

    public Appliance(String name, BigDecimal powerWatts) {
        this.name = name;
        this.powerWatts = powerWatts;
        this.createdAt = Instant.now();
    }

    public void updateDetails(String name, BigDecimal powerWatts) {
        this.name = name;
        this.powerWatts = powerWatts;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPowerWatts() {
        return powerWatts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
