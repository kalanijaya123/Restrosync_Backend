package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "stock_alerts")
public record StockAlert(
        @Id String id,
        String inventoryItemId,
        String itemName,
        Double currentStock,
        Double lowStockThreshold,
        String unit,
        String severity, // "warning", "critical"
        Boolean isResolved,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt) {
}
