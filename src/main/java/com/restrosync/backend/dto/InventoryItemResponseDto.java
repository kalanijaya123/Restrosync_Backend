package com.restrosync.backend.dto;

import lombok.Builder;

@Builder
public record InventoryItemResponseDto(
        String id,
        String name,
        String unit,
        Double currentStock,
        Double lowStockAlert,
        String category,
        Double costPrice) {
}
