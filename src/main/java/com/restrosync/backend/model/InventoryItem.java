// src/main/java/com/restrosync/backend/model/InventoryItem.java
package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "inventory")
public record InventoryItem(
                @Id String id,
                String name,
                String unit, // kg, liter, piece, gram, packet
                Double currentStock,
                Double lowStockAlert, // alert when stock ≤ this
                String category, // Vegetables, Meat, Spices, etc.
                Double costPrice // optional: for profit calculation
) {
        // Helper constructor for updates
        public InventoryItem withStock(Double newStock) {
                return new InventoryItem(id, name, unit, newStock, lowStockAlert, category, costPrice);
        }
}