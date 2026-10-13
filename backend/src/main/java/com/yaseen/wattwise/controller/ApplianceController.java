package com.yaseen.wattwise.controller;

import com.yaseen.wattwise.dto.ApplianceResponse;
import com.yaseen.wattwise.dto.CreateApplianceRequest;
import com.yaseen.wattwise.dto.EnergyCalculationResponse;
import com.yaseen.wattwise.dto.SavedApplianceCalculationRequest;
import com.yaseen.wattwise.dto.UpdateApplianceRequest;
import com.yaseen.wattwise.service.ApplianceService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appliances")
public class ApplianceController {

    private static final String VISITOR_ATTRIBUTE =
            "wattwise.visitorId";

    private final ApplianceService service;

    public ApplianceController(ApplianceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplianceResponse create(
            @Valid @RequestBody CreateApplianceRequest request,
            HttpSession session) {

        return service.create(visitorId(session), request);
    }

    @GetMapping
    public List<ApplianceResponse> findAll(HttpSession session) {
        return service.findAll(visitorId(session));
    }

    @GetMapping("/{id}")
    public ApplianceResponse findById(
            @PathVariable UUID id,
            HttpSession session) {

        return service.findById(visitorId(session), id);
    }

    @PutMapping("/{id}")
    public ApplianceResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplianceRequest request,
            HttpSession session) {

        return service.update(visitorId(session), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID id,
            HttpSession session) {

        service.delete(visitorId(session), id);
    }

    @PostMapping("/{id}/calculate")
    public EnergyCalculationResponse calculate(
            @PathVariable UUID id,
            @Valid @RequestBody SavedApplianceCalculationRequest request,
            HttpSession session) {

        return service.calculate(visitorId(session), id, request);
    }

    private UUID visitorId(HttpSession session) {
        synchronized (session) {
            UUID visitorId =
                    (UUID) session.getAttribute(VISITOR_ATTRIBUTE);

            if (visitorId == null) {
                visitorId = UUID.randomUUID();
                session.setAttribute(VISITOR_ATTRIBUTE, visitorId);
            }

            return visitorId;
        }
    }
}