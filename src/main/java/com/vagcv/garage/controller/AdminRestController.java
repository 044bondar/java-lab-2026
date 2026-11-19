package com.vagcv.garage.controller;

import com.vagcv.garage.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRestController {

    private final VehicleService vehicleService;

    @GetMapping("/stats")
    public Map<String, Object> getGarageStats(Authentication authentication) {
        return Map.of(
                "adminUser", authentication.getName(),
                "totalVehiclesInGarage", vehicleService.getAllVehicles().size(),
                "securityCheck", "Passed SecurityFilterChain hasRole('ADMIN')"
        );
    }
}
