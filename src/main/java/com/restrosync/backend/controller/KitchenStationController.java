package com.restrosync.backend.controller;

import com.restrosync.backend.model.KitchenStation;
import com.restrosync.backend.service.KitchenStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kitchen-stations")
@RequiredArgsConstructor
public class KitchenStationController {

    private final KitchenStationService kitchenStationService;

    /**
     * Get all active kitchen stations (for KDS display)
     */
    @GetMapping
    public ResponseEntity<List<KitchenStation>> getAllStations() {
        return ResponseEntity.ok(kitchenStationService.getAllActiveStations());
    }

    /**
     * Get stations for specific category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<KitchenStation>> getStationsForCategory(@PathVariable String category) {
        return ResponseEntity.ok(kitchenStationService.getStationsForCategory(category));
    }

    /**
     * Create new kitchen station (manager only)
     */
    @PostMapping
    public ResponseEntity<KitchenStation> createStation(
            @RequestParam String name,
            @RequestParam List<String> categories) {
        return ResponseEntity.ok(kitchenStationService.createStation(name, categories));
    }
}
