package com.yaseen.wattwise.service;

import com.yaseen.wattwise.dto.ApplianceCalculationResponse;
import com.yaseen.wattwise.dto.ApplianceUsageRequest;
import com.yaseen.wattwise.dto.HouseholdCalculationRequest;
import com.yaseen.wattwise.dto.HouseholdCalculationResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class HouseholdCalculator {

    private final EnergyCalculator energyCalculator;

    public HouseholdCalculator(EnergyCalculator energyCalculator) {
        this.energyCalculator = energyCalculator;
    }

    public HouseholdCalculationResponse calculate(
            HouseholdCalculationRequest request) {

        List<ApplianceCalculationResponse> results = new ArrayList<>();
        BigDecimal totalEnergy = BigDecimal.ZERO;

        for (ApplianceUsageRequest appliance : request.appliances()) {
            BigDecimal energy = energyCalculator.calculateEnergyKwh(
                    appliance.powerWatts(),
                    appliance.durationHours()
            );

            BigDecimal cost = energyCalculator.calculateCostGbp(
                    energy,
                    request.rateGbpPerKwh()
            );

            results.add(new ApplianceCalculationResponse(
                    appliance.name(),
                    energy,
                    cost
            ));

            totalEnergy = totalEnergy.add(energy);
        }

        BigDecimal totalCost = energyCalculator.calculateCostGbp(
                totalEnergy,
                request.rateGbpPerKwh()
        );

        return new HouseholdCalculationResponse(
                List.copyOf(results),
                totalEnergy,
                totalCost
        );
    }
}
