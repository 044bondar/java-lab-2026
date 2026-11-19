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

    // --- Бізнес-логіка рентабельності та підготовки авто для Lab 2 ---

    @Transactional(readOnly = true)
    public java.math.BigDecimal calculateProjectedProfit(Long vehicleId, java.math.BigDecimal repairCosts) {
        if (repairCosts == null || repairCosts.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Витрати на ремонт не можуть бути від'ємними");
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Автомобіль з ID " + vehicleId + " не знайдено"));

        if (vehicle.getPurchasePrice() == null || vehicle.getTargetResalePrice() == null) {
            throw new IllegalStateException("Для авто з ID " + vehicleId + " не задана ціна закупки або продажу");
        }

        return vehicle.getTargetResalePrice()
                .subtract(vehicle.getPurchasePrice())
                .subtract(repairCosts);
    }

    @Transactional(readOnly = true)
    public java.math.BigDecimal calculateProfitMarginPercent(Long vehicleId, java.math.BigDecimal repairCosts) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Автомобіль з ID " + vehicleId + " не знайдено"));

        if (vehicle.getTargetResalePrice() == null || vehicle.getTargetResalePrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Ціна продажу повинна бути більшою за нуль");
        }

        java.math.BigDecimal profit = calculateProjectedProfit(vehicleId, repairCosts);
        return profit.multiply(java.math.BigDecimal.valueOf(100))
                .divide(vehicle.getTargetResalePrice(), 2, java.math.RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public boolean isProfitableDeal(Long vehicleId, java.math.BigDecimal repairCosts, java.math.BigDecimal minDesiredProfit) {
        if (minDesiredProfit == null || minDesiredProfit.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Мінімальний бажаний прибуток не може бути від'ємним");
        }
        java.math.BigDecimal projectedProfit = calculateProjectedProfit(vehicleId, repairCosts);
        return projectedProfit.compareTo(minDesiredProfit) >= 0;
    }

    @Transactional
    public VehicleResponseDto markReadyForSale(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Автомобіль з ID " + vehicleId + " не знайдено"));

        if (vehicle.getStatus() == com.vagcv.garage.domain.VehicleStatus.SOLD) {
            throw new IllegalStateException("Автомобіль з ID " + vehicleId + " вже продано");
        }
        if (vehicle.getTargetResalePrice() == null || vehicle.getTargetResalePrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Неможливо виставити на продаж авто без ціни продажу");
        }

        vehicle.setStatus(com.vagcv.garage.domain.VehicleStatus.READY_FOR_SALE);
        vehicle.setUpdatedAt(java.time.LocalDateTime.now());
        return vehicleMapper.toDto(vehicleRepository.save(vehicle));
    }
}
