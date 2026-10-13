package com.yaseen.wattwise.controller;

import com.yaseen.wattwise.dto.EnergyCalculationResponse;
import com.yaseen.wattwise.dto.TimeBasedCalculationRequest;
import com.yaseen.wattwise.model.ScheduledAppliance;
import com.yaseen.wattwise.model.TariffPeriod;
import com.yaseen.wattwise.service.TimeBasedEnergyCalculator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/energy")
public class TimeBasedEnergyController {

    private final TimeBasedEnergyCalculator calculator;

    public TimeBasedEnergyController(TimeBasedEnergyCalculator calculator) {
        this.calculator = calculator;
    }

    @PostMapping("/calculate-scheduled")
    public EnergyCalculationResponse calculate(
            @Valid @RequestBody TimeBasedCalculationRequest request) {

        ScheduledAppliance appliance = new ScheduledAppliance(
                request.name(),
                request.powerWatts(),
                request.startMinute(),
                request.durationMinutes()
        );

        List<TariffPeriod> tariffs = request.tariffs().stream()
                .map(period -> new TariffPeriod(
                        period.startMinute(),
                        period.endMinute(),
                        period.rateGbpPerKwh()
                ))
                .toList();

        return calculator.calculate(appliance, tariffs);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidSchedule(
            IllegalArgumentException exception) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }
}
