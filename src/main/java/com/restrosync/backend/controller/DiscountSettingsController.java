package com.restrosync.backend.controller;

import com.restrosync.backend.model.DiscountSettingsConfig;
import com.restrosync.backend.service.AuthService;
import com.restrosync.backend.service.DiscountSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discount-settings")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174", "http://localhost:5175" })
@RequiredArgsConstructor
public class DiscountSettingsController {

    private final DiscountSettingsService discountSettingsService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<DiscountSettingsConfig> getSettings() {
        return ResponseEntity.ok(discountSettingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<?> saveSettings(@RequestHeader("userId") String userId,
            @RequestBody DiscountSettingsConfig settings) {
        if (!authService.isManager(userId)) {
            return ResponseEntity.status(403).body("Manager access required");
        }

        return ResponseEntity.ok(discountSettingsService.saveSettings(settings));
    }
}