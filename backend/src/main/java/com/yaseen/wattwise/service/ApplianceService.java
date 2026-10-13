package com.yaseen.wattwise.service;

import com.yaseen.wattwise.dto.ApplianceResponse;
import com.yaseen.wattwise.dto.CreateApplianceRequest;
import com.yaseen.wattwise.dto.EnergyCalculationResponse;
import com.yaseen.wattwise.dto.SavedApplianceCalculationRequest;
import com.yaseen.wattwise.dto.UpdateApplianceRequest;
import com.yaseen.wattwise.model.Appliance;
import com.yaseen.wattwise.repository.ApplianceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ApplianceService {

    private final ApplianceRepository repository;
    private final EnergyCalculator calculator;

    public ApplianceService(
            ApplianceRepository repository,
            EnergyCalculator calculator) {

        this.repository = repository;
        this.calculator = calculator;
    }

    @Transactional
    public ApplianceResponse create(
            UUID visitorId,
            CreateApplianceRequest request) {

        Appliance appliance = new Appliance(
                visitorId,
                request.name().strip(),
                request.powerWatts()
        );

        return toResponse(repository.save(appliance));
    }

    @Transactional(readOnly = true)
    public List<ApplianceResponse> findAll(UUID visitorId) {
        return repository.findAllByVisitorIdOrderByNameAscIdAsc(visitorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplianceResponse findById(UUID visitorId, UUID id) {
        return toResponse(findAppliance(visitorId, id));
    }

    @Transactional
    public ApplianceResponse update(
            UUID visitorId,
            UUID id,
            UpdateApplianceRequest request) {

        Appliance appliance = findAppliance(visitorId, id);

        appliance.updateDetails(
                request.name().strip(),
                request.powerWatts()
        );

        return toResponse(appliance);
    }

    @Transactional
    public void delete(UUID visitorId, UUID id) {
        Appliance appliance = findAppliance(visitorId, id);
        repository.delete(appliance);
    }

    @Transactional(readOnly = true)
    public EnergyCalculationResponse calculate(
            UUID visitorId,
            UUID id,
            SavedApplianceCalculationRequest request) {

        Appliance appliance = findAppliance(visitorId, id);

        BigDecimal energy = calculator.calculateEnergyKwh(
                appliance.getPowerWatts(),
                request.durationHours()
        );

        BigDecimal cost = calculator.calculateCostGbp(
                energy,
                request.rateGbpPerKwh()
        );

        return new EnergyCalculationResponse(energy, cost);
    }

    private Appliance findAppliance(UUID visitorId, UUID id) {
        return repository.findByIdAndVisitorId(id, visitorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Appliance not found"
                ));
    }

    private ApplianceResponse toResponse(Appliance appliance) {
        return new ApplianceResponse(
                appliance.getId(),
                appliance.getName(),
                appliance.getPowerWatts(),
                appliance.getCreatedAt()
        );
    }
}