package com.restrosync.backend.controller;

import com.restrosync.backend.dto.DeliveryFeeRequest;
import com.restrosync.backend.dto.DeliveryFeeResponse;
import com.restrosync.backend.model.DeliverySettingsConfig;
import com.restrosync.backend.model.User;
import com.restrosync.backend.repository.UserRepository;
import com.restrosync.backend.service.DeliverySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery-settings")
@RequiredArgsConstructor
public class DeliverySettingsController {

    private final DeliverySettingsService deliverySettingsService;
    private final UserRepository userRepository;

    @GetMapping
    public DeliverySettingsConfig getSettings() {
        return deliverySettingsService.getSettings();
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestHeader(value = "userId", required = false) String userId,
            @RequestBody DeliverySettingsConfig settings) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing user id");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.role() == null || !"Manager".equalsIgnoreCase(user.role().trim())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only managers can update delivery settings");
        }

        DeliverySettingsConfig saved = deliverySettingsService.saveSettings(settings);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/calculate")
    public DeliveryFeeResponse calculate(@RequestBody DeliveryFeeRequest request) {
        return deliverySettingsService.calculateFee(request.customerLatitude(), request.customerLongitude());
    }
}
