package com.restrosync.backend.controller;

import com.restrosync.backend.model.DiscountSettingsConfig;
import com.restrosync.backend.service.DiscountSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discount-settings")
@RequiredArgsConstructor
public class DiscountSettingsController {

    private final DiscountSettingsService discountSettingsService;

    @GetMapping
    public ResponseEntity<DiscountSettingsConfig> getSettings() {
        return ResponseEntity.ok(discountSettingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<DiscountSettingsConfig> saveSettings(@RequestBody DiscountSettingsConfig settings) {
        return ResponseEntity.ok(discountSettingsService.saveSettings(settings));
    }
}