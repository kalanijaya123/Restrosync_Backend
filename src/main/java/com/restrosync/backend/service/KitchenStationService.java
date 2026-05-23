package com.restrosync.backend.service;

import com.restrosync.backend.model.KitchenStation;
import com.restrosync.backend.repository.KitchenStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KitchenStationService {

    private final KitchenStationRepository kitchenStationRepository;

    /**
     * Get all active kitchen stations (KDS requirement 4.8)
     */
    public List<KitchenStation> getAllActiveStations() {
        return kitchenStationRepository.findByIsActiveTrueOrderByDisplayOrder();
    }

    /**
     * Get station by name
     */
    public Optional<KitchenStation> getStationByName(String name) {
        return kitchenStationRepository.findByName(name);
    }

    /**
     * Create new kitchen station
     */
    public KitchenStation createStation(String name, List<String> categories) {
        log.info("Creating kitchen station: {}", name);

        KitchenStation station = new KitchenStation(
                null,
                name,
                kitchenStationRepository.findAll().size() + 1,
                categories,
                true,
                0,
                LocalDateTime.now(),
                LocalDateTime.now());

        return kitchenStationRepository.save(station);
    }

    /**
     * Check if station handles a specific category
     */
    public boolean stationHandlesCategory(String stationId, String category) {
        return kitchenStationRepository.findById(stationId)
                .map(station -> station.menuItemCategories().contains(category))
                .orElse(false);
    }

    /**
     * Get all stations that handle a specific category
     */
    public List<KitchenStation> getStationsForCategory(String category) {
        return getAllActiveStations().stream()
                .filter(station -> station.menuItemCategories().contains(category))
                .toList();
    }

    /**
     * Update station order count
     */
    public void updateOrderCount(String stationId, Integer count) {
        kitchenStationRepository.findById(stationId).ifPresent(station -> {
            KitchenStation updated = new KitchenStation(
                    station.id(),
                    station.name(),
                    station.displayOrder(),
                    station.menuItemCategories(),
                    station.isActive(),
                    count,
                    station.createdAt(),
                    LocalDateTime.now());
            kitchenStationRepository.save(updated);
        });
    }
}
