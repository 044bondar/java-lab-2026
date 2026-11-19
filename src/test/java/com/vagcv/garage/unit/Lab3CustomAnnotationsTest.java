package com.vagcv.garage.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vagcv.garage.annotation.CurrentOperator;
import com.vagcv.garage.annotation.CurrentOperatorArgumentResolver;
import com.vagcv.garage.controller.VehicleRestController;
import com.vagcv.garage.domain.VehicleStatus;
import com.vagcv.garage.dto.VehicleRequestDto;
import com.vagcv.garage.dto.VehicleResponseDto;
import com.vagcv.garage.service.VehicleService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Lab 3: Тестування кастомних анотацій (Bean Validation, Meta-Annotation, ArgumentResolver)")
class Lab3CustomAnnotationsTest {

    private Validator validator;
    private CurrentOperatorArgumentResolver argumentResolver;
    private VehicleService vehicleService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        argumentResolver = new CurrentOperatorArgumentResolver();
        vehicleService = mock(VehicleService.class);
        objectMapper = new ObjectMapper();

        LocalValidatorFactoryBean springValidator = new LocalValidatorFactoryBean();
        springValidator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new VehicleRestController(vehicleService))
                .setCustomArgumentResolvers(argumentResolver)
                .setValidator(springValidator)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- AC 1, AC 2, AC 3: Bean Validation Constraint (@ProfitableResaleDeal) ---

    @Test
    @DisplayName("AC1 & AC2: @ProfitableResaleDeal автоматично викликається та пропускає вигідну угоду")
    void profitableResaleDeal_ValidMargin_ShouldPassValidation() {
        VehicleRequestDto validDto = VehicleRequestDto.builder()
                .vin("WVWZZZ3CZ8E123456")
                .brand("Volkswagen")
                .model("Passat B6 3.6 VR6")
                .manufactureYear(2008)
                .mileageKm(210000)
                .purchasePrice(new BigDecimal("6500.00"))
                .targetResalePrice(new BigDecimal("7200.00")) // прибуток 700 >= 200
                .status(VehicleStatus.READY_FOR_SALE)
                .build();

        Set<ConstraintViolation<VehicleRequestDto>> violations = validator.validate(validDto);

        assertTrue(violations.isEmpty(), "Для угоди з маржею >= 200 порушень бути не повинно");
    }

    @Test
    @DisplayName("AC1 & AC3: @ProfitableResaleDeal відхиляє малу маржу з детальним повідомленням")
    void profitableResaleDeal_InsufficientMargin_ShouldFailWithClearMessage() {
        VehicleRequestDto lowMarginDto = VehicleRequestDto.builder()
                .vin("WVWZZZ3CZ8E123456")
                .brand("Volkswagen")
                .model("Passat B6 3.6 VR6")
                .manufactureYear(2008)
                .mileageKm(210000)
                .purchasePrice(new BigDecimal("6500.00"))
                .targetResalePrice(new BigDecimal("6580.00")) // прибуток лише 80 < 200
                .status(VehicleStatus.READY_FOR_SALE)
                .build();

        Set<ConstraintViolation<VehicleRequestDto>> violations = validator.validate(lowMarginDto);

        assertEquals(1, violations.size());
        ConstraintViolation<VehicleRequestDto> violation = violations.iterator().next();
        assertEquals("targetResalePrice", violation.getPropertyPath().toString());
        assertTrue(violation.getMessage().contains("80.00"));
        assertTrue(violation.getMessage().contains("200.00"));
        assertTrue(violation.getMessage().contains("6700.00"));
    }

    // --- AC 6 & AC 7: HandlerMethodArgumentResolver (@CurrentOperator) ---

    @Test
    @DisplayName("AC6: supportsParameter повертає true лише для String з @CurrentOperator")
    void currentOperatorResolver_SupportsParameter_ShouldCheckAnnotationAndType() throws NoSuchMethodException {
        Method controllerMethod = VehicleRestController.class
                .getMethod("createVehicle", VehicleRequestDto.class, String.class);

        MethodParameter dtoParam = new MethodParameter(controllerMethod, 0);
        MethodParameter operatorParam = new MethodParameter(controllerMethod, 1);

        assertFalse(argumentResolver.supportsParameter(dtoParam));
        assertTrue(argumentResolver.supportsParameter(operatorParam));
    }

    @Test
    @DisplayName("AC7: resolveArgument витягує ім'я користувача з SecurityContextHolder")
    void currentOperatorResolver_ResolveArgument_ShouldExtractAuthenticatedUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "bohdan_admin",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        Method controllerMethod = VehicleRestController.class
                .getMethod("createVehicle", VehicleRequestDto.class, String.class);
        MethodParameter operatorParam = new MethodParameter(controllerMethod, 1);
        ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

        Object resolved = argumentResolver.resolveArgument(operatorParam, null, webRequest, null);

        assertEquals("bohdan_admin", resolved);
        assertTrue(operatorParam.hasParameterAnnotation(CurrentOperator.class));
    }

    // --- AC 4, AC 5, AC 8: Композитна анотація (@FleetCreatedPostMapping) + інтеграція в контролері ---

    @Test
    @DisplayName("AC4, AC5 & AC8: @FleetCreatedPostMapping повертає 201 CREATED, а @CurrentOperator передається у сервіс")
    void createVehicleEndpoint_ShouldApplyMetaAnnotationAndInjectOperator() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "taras_mechanic",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        VehicleRequestDto requestDto = VehicleRequestDto.builder()
                .vin("WVWZZZ1KZ9W111222")
                .brand("Volkswagen")
                .model("Golf R32")
                .manufactureYear(2008)
                .mileageKm(165000)
                .purchasePrice(new BigDecimal("9500.00"))
                .targetResalePrice(new BigDecimal("11800.00"))
                .status(VehicleStatus.READY_FOR_SALE)
                .build();

        VehicleResponseDto responseDto = VehicleResponseDto.builder()
                .id(10L)
                .vin("WVWZZZ1KZ9W111222")
                .brand("Volkswagen")
                .model("Golf R32")
                .manufactureYear(2008)
                .mileageKm(165000)
                .purchasePrice(new BigDecimal("9500.00"))
                .targetResalePrice(new BigDecimal("11800.00"))
                .expectedProfit(new BigDecimal("2300.00"))
                .status(VehicleStatus.READY_FOR_SALE)
                .createdBy("taras_mechanic")
                .updatedAt(LocalDateTime.now())
                .build();

        when(vehicleService.createVehicle(any(VehicleRequestDto.class), eq("taras_mechanic")))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.createdBy").value("taras_mechanic"));

        verify(vehicleService, times(1)).createVehicle(any(VehicleRequestDto.class), eq("taras_mechanic"));
    }

    @Test
    @DisplayName("AC3 (HTTP): POST /api/vehicles повертає 400 Bad Request при порушенні @ProfitableResaleDeal")
    void createVehicleEndpoint_InvalidDeal_ShouldReturnBadRequest() throws Exception {
        VehicleRequestDto badDealDto = VehicleRequestDto.builder()
                .vin("WVWZZZ1KZ9W111222")
                .brand("Volkswagen")
                .model("Golf R32")
                .manufactureYear(2008)
                .mileageKm(165000)
                .purchasePrice(new BigDecimal("9500.00"))
                .targetResalePrice(new BigDecimal("9550.00")) // прибуток 50 < 200
                .status(VehicleStatus.READY_FOR_SALE)
                .build();

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDealDto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vehicleService);
    }
}