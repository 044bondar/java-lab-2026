package com.vagcv.garage;

import com.vagcv.garage.aop.DealAuditRegistry;
import com.vagcv.garage.domain.Vehicle;
import com.vagcv.garage.domain.VehicleStatus;
import com.vagcv.garage.dto.VehicleRequestDto;
import com.vagcv.garage.repository.VehicleRepository;
import com.vagcv.garage.service.VehicleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Lab 4 (AOP): Перевірка проксі-аспектів, Guard-обмеження та self-invocation")
class Lab4AopProxyAndSelfInvocationTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DealAuditRegistry auditRegistry;

    private Long savedVehicleId;

    @BeforeEach
    void setUp() {
        auditRegistry.clear();
        vehicleRepository.deleteAll();

        Vehicle vehicle = new Vehicle();
        vehicle.setVin("WVWZZZ3CZ8E554433");
        vehicle.setBrand("Volkswagen");
        vehicle.setModel("Passat B6 R36");
        vehicle.setManufactureYear(2009);
        vehicle.setMileageKm(190000);
        vehicle.setPurchasePrice(new BigDecimal("8500.00"));
        vehicle.setTargetResalePrice(new BigDecimal("11200.00"));
        vehicle.setStatus(VehicleStatus.READY_FOR_SALE);
        vehicle.setCreatedBy("bohdan_admin");
        vehicle.setUpdatedAt(LocalDateTime.now());

        savedVehicleId = vehicleRepository.save(vehicle).getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Сценарій 1-3: Зовнішній виклик через Spring Proxy активує @AuditDealOperation")
    void externalCall_ThroughProxy_ShouldTriggerAuditAspect() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin_user", "N/A", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        vehicleService.markReadyForSale(savedVehicleId);

        assertEquals(1, auditRegistry.getAuditEntries().size(),
                "Зовнішній виклик через проксі повинен створити 1 запис аудиту");
        DealAuditRegistry.AuditEntry entry = auditRegistry.getAuditEntries().get(0);
        assertEquals("MARK_READY_FOR_SALE", entry.action());
        assertEquals(savedVehicleId, entry.vehicleId());
        assertEquals("admin_user", entry.operator());
    }

    @Test
    @DisplayName("Сценарій 4 (Self-invocation): Внутрішній виклик this.markReadyForSale() НЕ активує аспект")
    void internalSelfInvocation_BypassingProxy_ShouldNotTriggerAuditAspect() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin_user", "N/A", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        vehicleService.prepareAndPublishInternal(savedVehicleId);

        Vehicle updated = vehicleRepository.findById(savedVehicleId).orElseThrow();
        assertEquals(VehicleStatus.READY_FOR_SALE, updated.getStatus(),
                "Бізнес-логіка методу повинна виконатися успішно");
        assertTrue(auditRegistry.getAuditEntries().isEmpty(),
                "Через self-invocation (this.markReadyForSale) аспект не повинен спрацювати");
    }

    @Test
    @DisplayName("Guard Aspect (@Before): Блокує створення авто дорожче 10000 без ролі ROLE_ADMIN, але фіксує SLA")
    void guardHighValueResale_WithoutAdminRole_ShouldThrowAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "mechanic_user", "N/A", List.of(new SimpleGrantedAuthority("ROLE_MECHANIC"))
                )
        );

        VehicleRequestDto expensiveCar = VehicleRequestDto.builder()
                .vin("WAUZZZ8K9AA001122")
                .brand("Audi")
                .model("S4 B8")
                .manufactureYear(2011)
                .mileageKm(145000)
                .purchasePrice(new BigDecimal("11000.00"))
                .targetResalePrice(new BigDecimal("13500.00"))
                .status(VehicleStatus.READY_FOR_SALE)
                .build();

        assertThrows(AccessDeniedException.class,
                () -> vehicleService.createVehicle(expensiveCar, "mechanic_user"));
        assertEquals(1, auditRegistry.getSlaMetrics().size(),
                "@Around SLA-аспект повинен зафіксувати метрику навіть при винятку");
    }
}