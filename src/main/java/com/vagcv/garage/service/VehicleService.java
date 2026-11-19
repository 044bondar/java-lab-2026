package com.vagcv.garage.service;

import com.vagcv.garage.domain.Vehicle;
import com.vagcv.garage.dto.VehicleRequestDto;
import com.vagcv.garage.dto.VehicleResponseDto;
import com.vagcv.garage.exception.VehicleNotFoundException;
import com.vagcv.garage.mapper.VehicleMapper;
import com.vagcv.garage.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Transactional(readOnly = true)
    public List<VehicleResponseDto> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(vehicleMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponseDto getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException("Автомобіль з ID=" + id + " не знайдено"));
        return vehicleMapper.toDto(vehicle);
    }

    @Transactional
    public VehicleResponseDto createVehicle(VehicleRequestDto requestDto, String operatorName) {
        if (vehicleRepository.existsByVin(requestDto.getVin())) {
            throw new IllegalArgumentException("Автомобіль із VIN-кодом " + requestDto.getVin() + " вже існує в базі");
        }
        Vehicle entity = vehicleMapper.toEntity(requestDto);
        entity.setCreatedBy(operatorName != null ? operatorName : "unknown");
        Vehicle saved = vehicleRepository.save(entity);
        return vehicleMapper.toDto(saved);
    }

    @Transactional
    public VehicleResponseDto updateVehicle(Long id, VehicleRequestDto requestDto) {
        Vehicle existing = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException("Автомобіль з ID=" + id + " не знайдено"));

        if (!existing.getVin().equals(requestDto.getVin()) && vehicleRepository.existsByVin(requestDto.getVin())) {
            throw new IllegalArgumentException("Інший автомобіль уже зареєстрований із VIN=" + requestDto.getVin());
        }

        vehicleMapper.updateEntityFromDto(requestDto, existing);
        Vehicle updated = vehicleRepository.save(existing);
        return vehicleMapper.toDto(updated);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteVehicle(Long id) {
        Vehicle existing = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException("Автомобіль з ID=" + id + " не знайдено"));
        vehicleRepository.delete(existing);
    }
}
