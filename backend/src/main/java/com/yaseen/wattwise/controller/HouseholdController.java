package com.yaseen.wattwise.controller;

import com.yaseen.wattwise.dto.HouseholdCalculationRequest;
import com.yaseen.wattwise.dto.HouseholdCalculationResponse;
import com.yaseen.wattwise.service.HouseholdCalculator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/household")
public class HouseholdController {

    private final HouseholdCalculator calculator;

    public HouseholdController(HouseholdCalculator calculator) {
        this.calculator = calculator;
    }

    @PostMapping("/calculate")
    public HouseholdCalculationResponse calculate(
            @Valid @RequestBody HouseholdCalculationRequest request) {

        return calculator.calculate(request);
    }
}