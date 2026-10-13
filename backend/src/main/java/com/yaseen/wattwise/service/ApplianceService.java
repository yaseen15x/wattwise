package com.yaseen.wattwise.service;

import com.yaseen.wattwise.dto.ApplianceResponse;
import com.yaseen.wattwise.dto.CreateApplianceRequest;
import com.yaseen.wattwise.model.Appliance;
import com.yaseen.wattwise.repository.ApplianceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApplianceService {

    private final ApplianceRepository repository;

    public ApplianceService(ApplianceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ApplianceResponse create(CreateApplianceRequest request) {
        Appliance appliance = new Appliance(
                request.name().strip(),
                request.powerWatts()
        );

        Appliance saved = repository.save(appliance);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApplianceResponse> findAll() {
        return repository.findAll(Sort.by("name", "id"))
                .stream()
                .map(this::toResponse)
                .toList();
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
