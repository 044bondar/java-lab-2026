package com.vagcv.garage.controller;

import com.vagcv.garage.security.DevJwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthDevController {

    private final DevJwtTokenService tokenService;

    @GetMapping("/dev-token")
    public Map<String, Object> issueDevToken(
            @RequestParam(defaultValue = "bohdan_admin") String username,
            @RequestParam(defaultValue = "ADMIN") List<String> roles) {
        String token = tokenService.generateToken(username, roles);
        return Map.of(
                "username", username,
                "roles", roles,
                "token_type", "Bearer",
                "access_token", token
        );
    }
}
