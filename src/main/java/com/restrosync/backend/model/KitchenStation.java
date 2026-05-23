package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "kitchen_stations")
public record KitchenStation(
        @Id String id,
        String name, // "Main", "Grill", "Prep", "Drinks", "Expedite"
        Integer displayOrder,
        List<String> menuItemCategories, // Categories this station handles
        Boolean isActive,
        Integer currentOrderCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
