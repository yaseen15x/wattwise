package com.yaseen.wattwise.controller;

import com.yaseen.wattwise.dto.ApplianceResponse;
import com.yaseen.wattwise.dto.CreateApplianceRequest;
import com.yaseen.wattwise.service.ApplianceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/appliances")
public class ApplianceController {

    private final ApplianceService service;

    public ApplianceController(ApplianceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplianceResponse create(
            @Valid @RequestBody CreateApplianceRequest request) {

        return service.create(request);
    }

    @GetMapping
    public List<ApplianceResponse> findAll() {
        return service.findAll();
    }
}
