package com.yaseen.wattwise.controller;

import com.yaseen.wattwise.dto.EnergyCalculationRequest;
import com.yaseen.wattwise.dto.EnergyCalculationResponse;
import com.yaseen.wattwise.service.EnergyCalculator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/energy")
public class EnergyController {

    private final EnergyCalculator calculator;

    public EnergyController(EnergyCalculator calculator) {
        this.calculator = calculator;
    }

    @PostMapping("/calculate")
    public EnergyCalculationResponse calculate(
            @Valid @RequestBody EnergyCalculationRequest request) {

        BigDecimal energy = calculator.calculateEnergyKwh(
                request.powerWatts(),
                request.durationHours()
        );

        BigDecimal cost = calculator.calculateCostGbp(
                energy,
                request.rateGbpPerKwh()
        );

        return new EnergyCalculationResponse(energy, cost);
    }
}
