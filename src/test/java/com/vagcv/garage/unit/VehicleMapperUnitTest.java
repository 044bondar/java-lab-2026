package com.vagcv.garage.unit;

import com.vagcv.garage.domain.Vehicle;
import com.vagcv.garage.domain.VehicleStatus;
import com.vagcv.garage.dto.VehicleRequestDto;
import com.vagcv.garage.dto.VehicleResponseDto;
import com.vagcv.garage.mapper.VehicleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit-тести VehicleMapper (MapStruct)")
class VehicleMapperUnitTest {

    private VehicleMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(VehicleMapper.class);
    }

    @Test
    @DisplayName("toEntity: коректно мапить VehicleRequestDto у сутність Vehicle")
    void toEntity_ShouldMapAllFields() {
        VehicleRequestDto request = VehicleRequestDto.builder()
                .vin("WVWZZZ1KZ9W111222")
                .brand("Volkswagen")
                .model("Golf R32")
                .manufactureYear(2008)
                .mileageKm(165000)
                .purchasePrice(new BigDecimal("9500.00"))
                .targetResalePrice(new BigDecimal("11800.00"))
                .status(VehicleStatus.READY_FOR_SALE)
                .build();

        Vehicle entity = mapper.toEntity(request);

        assertNotNull(entity);
        assertEquals("WVWZZZ1KZ9W111222", entity.getVin());
        assertEquals("Volkswagen", entity.getBrand());
        assertEquals("Golf R32", entity.getModel());
        assertEquals(2008, entity.getManufactureYear());
        assertEquals(165000, entity.getMileageKm());
        assertEquals(new BigDecimal("9500.00"), entity.getPurchasePrice());
        assertEquals(new BigDecimal("11800.00"), entity.getTargetResalePrice());
        assertEquals(VehicleStatus.READY_FOR_SALE, entity.getStatus());
    }

    @Test
    @DisplayName("toDto: коректно мапить Vehicle у DTO VehicleResponseDto")
    void toDto_ShouldMapAllFields() {
        LocalDateTime timestamp = LocalDateTime.now();

        Vehicle vehicle = new Vehicle();
        vehicle.setId(5L);
        vehicle.setVin("TMBJJ7NS8K8012345");
        vehicle.setBrand("Skoda");
        vehicle.setModel("Octavia RS");
        vehicle.setManufactureYear(2019);
        vehicle.setMileageKm(98000);
        vehicle.setPurchasePrice(new BigDecimal("14200.00"));
        vehicle.setTargetResalePrice(new BigDecimal("16500.00"));
        vehicle.setStatus(VehicleStatus.READY_FOR_SALE);
        vehicle.setCreatedBy("taras_mechanic");
        vehicle.setUpdatedAt(timestamp);

        VehicleResponseDto response = mapper.toDto(vehicle);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("TMBJJ7NS8K8012345", response.getVin());
        assertEquals("Skoda", response.getBrand());
        assertEquals("Octavia RS", response.getModel());
        assertEquals(2019, response.getManufactureYear());
        assertEquals(98000, response.getMileageKm());
        assertEquals(new BigDecimal("14200.00"), response.getPurchasePrice());
        assertEquals(new BigDecimal("16500.00"), response.getTargetResalePrice());
        assertEquals(VehicleStatus.READY_FOR_SALE, response.getStatus());
        assertEquals("taras_mechanic", response.getCreatedBy());
        assertEquals(timestamp, response.getUpdatedAt());
    }
}