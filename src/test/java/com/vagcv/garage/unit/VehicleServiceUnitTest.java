package com.vagcv.garage.unit;

import com.vagcv.garage.domain.Vehicle;
import com.vagcv.garage.domain.VehicleStatus;
import com.vagcv.garage.dto.VehicleResponseDto;
import com.vagcv.garage.mapper.VehicleMapper;
import com.vagcv.garage.repository.VehicleRepository;
import com.vagcv.garage.service.VehicleService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-тести VehicleService (JUnit 5 + Mockito)")
class VehicleServiceUnitTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleMapper vehicleMapper;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle sampleVehicle;
    private VehicleResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        sampleVehicle = new Vehicle();
        sampleVehicle.setId(1L);
        sampleVehicle.setVin("WVWZZZ3CZ8E123456");
        sampleVehicle.setBrand("Volkswagen");
        sampleVehicle.setModel("Passat B6 3.6 VR6");
        sampleVehicle.setManufactureYear(2008);
        sampleVehicle.setMileageKm(215000);
        sampleVehicle.setPurchasePrice(new BigDecimal("6500.00"));
        sampleVehicle.setTargetResalePrice(new BigDecimal("8400.00"));
        sampleVehicle.setStatus(VehicleStatus.READY_FOR_SALE);
        sampleVehicle.setCreatedBy("bohdan_admin");
        sampleVehicle.setUpdatedAt(now);

        sampleResponse = VehicleResponseDto.builder()
                .id(1L)
                .vin("WVWZZZ3CZ8E123456")
                .brand("Volkswagen")
                .model("Passat B6 3.6 VR6")
                .manufactureYear(2008)
                .mileageKm(215000)
                .purchasePrice(new BigDecimal("6500.00"))
                .targetResalePrice(new BigDecimal("8400.00"))
                .expectedProfit(new BigDecimal("1900.00"))
                .status(VehicleStatus.READY_FOR_SALE)
                .createdBy("bohdan_admin")
                .updatedAt(now)
                .build();
    }

    @ParameterizedTest(name = "Ремонт {0} -> чистий прибуток {1}")
    @CsvSource({
            "0.00, 1900.00",
            "550.00, 1350.00",
            "1900.00, 0.00",
            "2200.00, -300.00"
    })
    @DisplayName("calculateProjectedProfit: параметризований розрахунок чистого прибутку")
    void calculateProjectedProfit_ShouldReturnExpectedValue(String repairStr, String expectedStr) {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));

        BigDecimal profit = vehicleService.calculateProjectedProfit(1L, new BigDecimal(repairStr));

        assertEquals(0, new BigDecimal(expectedStr).compareTo(profit));
    }

    @Test
    @DisplayName("calculateProjectedProfit: кидає EntityNotFoundException, якщо авто не знайдено")
    void calculateProjectedProfit_WhenNotFound_ShouldThrow() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> vehicleService.calculateProjectedProfit(99L, new BigDecimal("100.00"))
        );
    }

    @Test
    @DisplayName("calculateProjectedProfit: кидає IllegalArgumentException при від'ємних витратах")
    void calculateProjectedProfit_NegativeCost_ShouldThrow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> vehicleService.calculateProjectedProfit(1L, new BigDecimal("-50.00"))
        );
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    @DisplayName("calculateProjectedProfit: кидає IllegalStateException, якщо не задані ціни")
    void calculateProjectedProfit_MissingPrices_ShouldThrow() {
        sampleVehicle.setTargetResalePrice(null);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));

        assertThrows(
                IllegalStateException.class,
                () -> vehicleService.calculateProjectedProfit(1L, new BigDecimal("100.00"))
        );
    }

    @Test
    @DisplayName("calculateProfitMarginPercent: обчислює рентабельність у відсотках")
    void calculateProfitMarginPercent_ShouldCalculatePercentage() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));

        BigDecimal margin = vehicleService.calculateProfitMarginPercent(1L, new BigDecimal("220.00"));

        assertEquals(0, new BigDecimal("20.00").compareTo(margin));
    }

    @Test
    @DisplayName("isProfitableDeal: перевіряє досягнення мінімальної бажаної маржі")
    void isProfitableDeal_ShouldEvaluateThreshold() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));

        assertTrue(vehicleService.isProfitableDeal(1L, new BigDecimal("400.00"), new BigDecimal("1500.00")));
        assertFalse(vehicleService.isProfitableDeal(1L, new BigDecimal("600.00"), new BigDecimal("1500.00")));
    }

    @Test
    @DisplayName("isProfitableDeal: кидає IllegalArgumentException при від'ємному порозі прибутку")
    void isProfitableDeal_NegativeDesiredProfit_ShouldThrow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> vehicleService.isProfitableDeal(1L, new BigDecimal("100.00"), new BigDecimal("-10.00"))
        );
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    @DisplayName("markReadyForSale: переводить статус авто в READY_FOR_SALE")
    void markReadyForSale_ValidVehicle_ShouldChangeStatus() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleMapper.toDto(any(Vehicle.class))).thenReturn(sampleResponse);

        VehicleResponseDto result = vehicleService.markReadyForSale(1L);

        assertNotNull(result);
        assertEquals(VehicleStatus.READY_FOR_SALE, sampleVehicle.getStatus());
        verify(vehicleRepository).save(sampleVehicle);
    }

    @Test
    @DisplayName("markReadyForSale: забороняє повторне виставлення проданого авто")
    void markReadyForSale_AlreadySold_ShouldThrow() {
        sampleVehicle.setStatus(VehicleStatus.SOLD);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));

        assertThrows(IllegalStateException.class, () -> vehicleService.markReadyForSale(1L));
        verify(vehicleRepository, never()).save(any());
    }
}
