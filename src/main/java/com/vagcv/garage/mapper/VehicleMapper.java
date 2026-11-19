package com.vagcv.garage.mapper;

import com.vagcv.garage.domain.Vehicle;
import com.vagcv.garage.dto.VehicleRequestDto;
import com.vagcv.garage.dto.VehicleResponseDto;
import org.mapstruct.*;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Vehicle toEntity(VehicleRequestDto dto);

    @Mapping(target = "expectedProfit", expression = "java(calculateProfit(entity))")
    VehicleResponseDto toDto(Vehicle entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(VehicleRequestDto dto, @MappingTarget Vehicle entity);

    default BigDecimal calculateProfit(Vehicle entity) {
        if (entity == null || entity.getTargetResalePrice() == null || entity.getPurchasePrice() == null) {
            return BigDecimal.ZERO;
        }
        return entity.getTargetResalePrice().subtract(entity.getPurchasePrice());
    }
}
