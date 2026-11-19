package com.vagcv.garage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vagcv.garage.domain.VehicleStatus;
import com.vagcv.garage.dto.VehicleRequestDto;
import com.vagcv.garage.security.DevJwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Lab1SecurityAndCrudTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DevJwtTokenService tokenService;

    @Test
    @DisplayName("AC4: Неавтентифікований запит до UI (/ui/vehicles) перенаправляє на OIDC логін")
    void unauthenticatedUiRequest_redirectsToOidcLogin() throws Exception {
        mockMvc.perform(get("/ui/vehicles"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("AC5: Запит до API без JWT повертає 401, а з дійсним JWT повертає 200 та дані з Liquibase")
    void apiVehicles_requiresValidJwtAndReturnsLiquibaseSeedData() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isUnauthorized());

        String mechanicToken = tokenService.generateToken("taras", List.of("MECHANIC"));

        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", "Bearer " + mechanicToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brand").value("Volkswagen"));
    }

    @Test
    @DisplayName("AC6: SecurityFilterChain блокує MECHANIC на /api/admin/stats (403) і пропускає ADMIN (200)")
    void adminEndpoint_enforcesSecurityFilterChainRoleRule() throws Exception {
        String mechanicToken = tokenService.generateToken("taras", List.of("MECHANIC"));
        String adminToken = tokenService.generateToken("bohdan", List.of("ADMIN"));

        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + mechanicToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminUser").value("bohdan"));
    }

    @Test
    @DisplayName("AC1 та AC7: CRUD створення працює, а @PreAuthorize блокує видалення для MECHANIC (403)")
    void createAndDeleteVehicle_enforcesPreAuthorizeOnService() throws Exception {
        String mechanicToken = tokenService.generateToken("taras", List.of("MECHANIC"));
        String adminToken = tokenService.generateToken("bohdan", List.of("ADMIN"));

        VehicleRequestDto newCar = VehicleRequestDto.builder()
                .vin("WAUZZZ8K9AA012345")
                .brand("Audi")
                .model("A4 B8 2.0 TDI")
                .manufactureYear(2010)
                .mileageKm(230000)
                .purchasePrice(new BigDecimal("7000.00"))
                .targetResalePrice(new BigDecimal("8600.00"))
                .status(VehicleStatus.INSPECTION)
                .build();

        String responseJson = mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", "Bearer " + mechanicToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCar)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expectedProfit").value(1600.00))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(responseJson).get("id").asLong();

        mockMvc.perform(delete("/api/vehicles/" + createdId)
                        .header("Authorization", "Bearer " + mechanicToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/vehicles/" + createdId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}
