package com.vagcv.garage.controller;

import com.vagcv.garage.annotation.CurrentOperator;
import com.vagcv.garage.annotation.FleetCreatedPostMapping;
import com.vagcv.garage.dto.VehicleRequestDto;
import com.vagcv.garage.dto.VehicleResponseDto;
import com.vagcv.garage.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleRestController {

    private final VehicleService vehicleService;

    @GetMapping
    public List<VehicleResponseDto> getAllVehicles() {
        return vehicleService.getAllVehicles();
    }

    @GetMapping("/{id}")
    public VehicleResponseDto getVehicleById(@PathVariable Long id) {
        return vehicleService.getVehicleById(id);
    }

    @FleetCreatedPostMapping
    public VehicleResponseDto createVehicle(
            @Valid @RequestBody VehicleRequestDto requestDto,
            @CurrentOperator(fallback = "api_user") String operator) {
        return vehicleService.createVehicle(requestDto, operator);
    }

    @PutMapping("/{id}")
    public VehicleResponseDto updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequestDto requestDto) {
        return vehicleService.updateVehicle(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
    }
}