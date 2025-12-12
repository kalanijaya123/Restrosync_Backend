package com.restrosync.backend.mapper;

import com.restrosync.backend.dto.InventoryItemCreateDto;
import com.restrosync.backend.dto.InventoryItemResponseDto;
import com.restrosync.backend.model.InventoryItem;

public class InventoryItemMapper {

    public static InventoryItem toEntity(InventoryItemCreateDto dto) {
        return new InventoryItem(
                null,
                dto.name(),
                dto.unit(),
                dto.currentStock(),
                dto.lowStockAlert(),
                dto.category(),
                dto.costPrice());
    }

    public static InventoryItemResponseDto toResponseDto(InventoryItem item) {
        return InventoryItemResponseDto.builder()
                .id(item.id())
                .name(item.name())
                .unit(item.unit())
                .currentStock(item.currentStock())
                .lowStockAlert(item.lowStockAlert())
                .category(item.category())
                .costPrice(item.costPrice())
                .build();
    }
}
