package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "delivery_settings")
public record DeliverySettingsConfig(
        @Id String id,
        Double restaurantLatitude,
        Double restaurantLongitude,
        List<DistanceFeeRange> ranges,
        LocalDateTime updatedAt) {

    public record DistanceFeeRange(
            Double maxDistanceKm,
            Double fee) {
    }
}
