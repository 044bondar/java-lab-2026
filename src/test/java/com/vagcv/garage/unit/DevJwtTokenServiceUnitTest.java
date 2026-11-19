package com.vagcv.garage.unit;

import com.vagcv.garage.security.DevJwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit-тести DevJwtTokenService")
class DevJwtTokenServiceUnitTest {

    private DevJwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new DevJwtTokenService();
        ReflectionTestUtils.setField(
                tokenService,
                "secret",
                "vag-cv-super-secret-signing-key-2026-min-32-bytes!"
        );
    }

    @Test
    @DisplayName("generateToken: створює валідний 3-частинний JWT із subject та списком ролей")
    void generateToken_ShouldContainSubjectAndRoles() {
        String rawToken = tokenService.generateToken("bohdan_admin", List.of("ADMIN", "MECHANIC"));

        assertNotNull(rawToken);
        String[] parts = rawToken.split("\\.");
        assertEquals(3, parts.length);

        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        assertTrue(headerJson.contains("\"alg\":\"HS256\""));
        assertTrue(payloadJson.contains("\"sub\":\"bohdan_admin\""));
        assertTrue(payloadJson.contains("\"preferred_username\":\"bohdan_admin\""));
        assertTrue(payloadJson.contains("\"roles\":[\"ADMIN\",\"MECHANIC\"]"));
        assertFalse(parts[2].isBlank());
    }

    @Test
    @DisplayName("generateToken: кидає IllegalStateException, якщо секретний ключ не ініціалізовано")
    void generateToken_WhenSecretMissing_ShouldThrowIllegalState() {
        DevJwtTokenService unconfiguredService = new DevJwtTokenService();

        assertThrows(
                IllegalStateException.class,
                () -> unconfiguredService.generateToken("bohdan_admin", List.of("ADMIN"))
        );
    }
}