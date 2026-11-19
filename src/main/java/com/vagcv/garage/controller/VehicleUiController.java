package com.vagcv.garage.controller;

import com.vagcv.garage.domain.VehicleStatus;
import com.vagcv.garage.dto.VehicleRequestDto;
import com.vagcv.garage.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class VehicleUiController {

    private final VehicleService vehicleService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/ui/vehicles")
    public String listVehicles(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("newVehicle", new VehicleRequestDto());
        model.addAttribute("statuses", VehicleStatus.values());
        model.addAttribute("username", oidcUser != null ? oidcUser.getPreferredUsername() : "Operator");
        return "vehicles";
    }

    @PostMapping("/ui/vehicles")
    public String createVehicleFromUi(
            @Valid @ModelAttribute("newVehicle") VehicleRequestDto dto,
            BindingResult bindingResult,
            Model model,
            @AuthenticationPrincipal OidcUser oidcUser) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("vehicles", vehicleService.getAllVehicles());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("username", oidcUser != null ? oidcUser.getPreferredUsername() : "Operator");
            return "vehicles";
        }
        String user = oidcUser != null ? oidcUser.getPreferredUsername() : "ui_user";
        vehicleService.createVehicle(dto, user);
        return "redirect:/ui/vehicles";
    }

    @PostMapping("/ui/vehicles/{id}/delete")
    public String deleteVehicleFromUi(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return "redirect:/ui/vehicles";
    }
}
