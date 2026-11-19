package com.vagcv.garage.dto;

import com.vagcv.garage.domain.VehicleStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponseDto {
    private Long id;
    private String vin;
    private String brand;
    private String model;
    private Integer manufactureYear;
    private Integer mileageKm;
    private BigDecimal purchasePrice;
    private BigDecimal targetResalePrice;
    private BigDecimal expectedProfit;
    private VehicleStatus status;
    private String createdBy;
    private LocalDateTime updatedAt;
}
