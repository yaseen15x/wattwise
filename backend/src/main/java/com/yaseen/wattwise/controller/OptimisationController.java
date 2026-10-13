package com.yaseen.wattwise.controller;

import com.yaseen.wattwise.dto.OptimisationRequest;
import com.yaseen.wattwise.dto.OptimisationResponse;
import com.yaseen.wattwise.dto.TimeBasedCalculationRequest;
import com.yaseen.wattwise.model.OptimisationWindow;
import com.yaseen.wattwise.model.ScheduledAppliance;
import com.yaseen.wattwise.model.TariffPeriod;
import com.yaseen.wattwise.service.ScheduleOptimiser;
import com.yaseen.wattwise.service.TimeBasedEnergyCalculator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/energy")
public class OptimisationController {

    private final ScheduleOptimiser optimiser;
    private final TimeBasedEnergyCalculator calculator;

    public OptimisationController(
            ScheduleOptimiser optimiser,
            TimeBasedEnergyCalculator calculator) {

        this.optimiser = optimiser;
        this.calculator = calculator;
    }

    @PostMapping("/optimise")
    public OptimisationResponse optimise(
            @Valid @RequestBody OptimisationRequest request) {

        TimeBasedCalculationRequest schedule = request.schedule();

        ScheduledAppliance original = new ScheduledAppliance(
                schedule.name(),
                schedule.powerWatts(),
                schedule.startMinute(),
                schedule.durationMinutes()
        );

        OptimisationWindow window = new OptimisationWindow(
                request.earliestStartMinute(),
                request.latestFinishMinute()
        );

        List<TariffPeriod> tariffs = schedule.tariffs().stream()
                .map(period -> new TariffPeriod(
                        period.startMinute(),
                        period.endMinute(),
                        period.rateGbpPerKwh()
                ))
                .toList();

        ScheduledAppliance recommended =
                optimiser.optimise(original, window, tariffs);

        BigDecimal originalCost =
                calculator.calculate(original, tariffs).costGbp();

        BigDecimal recommendedCost =
                calculator.calculate(recommended, tariffs).costGbp();

        boolean originalMeetsWindow =
                original.startMinute() >= window.earliestStartMinute()
                && original.startMinute() + original.durationMinutes()
                <= window.latestFinishMinute();

        return new OptimisationResponse(
                original.name(),
                original.startMinute(),
                recommended.startMinute(),
                original.durationMinutes(),
                originalMeetsWindow,
                originalCost,
                recommendedCost,
                originalCost.subtract(recommendedCost)
        );
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