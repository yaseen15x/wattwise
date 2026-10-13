package com.yaseen.wattwise.repository;

import com.yaseen.wattwise.model.Appliance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplianceRepository
        extends JpaRepository<Appliance, UUID> {
}
