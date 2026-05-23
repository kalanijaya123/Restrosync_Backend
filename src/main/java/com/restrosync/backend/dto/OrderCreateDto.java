package com.restrosync.backend.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderCreateDto(
                Integer orderNo,
                String tableId,
                String tableNumber,
                String source,
                List<OrderItemDto> items,
                Double total,
                String status,
                LocalDateTime servedAt,
                String paymentStatus,
                String customerName,
                String customerPhone,
                String notes,
                String waiterName,
                String kotToken) {
        @Builder
        public record OrderItemDto(
                        String menuItemId,
                        String menuItemName,
                        String sizeName,
                        Double basePrice,
                        Integer qty,
                        List<SelectedExtraDto> extras,
                        Double chickenUsed,
                        Double riceUsed,
                        Double cheeseUsed,
                        Double totalIngredientCost) {
        }

        @Builder
        public record SelectedExtraDto(
                        String extraId,
                        String name,
                        Double price,
                        Integer qty,
                        Double quantityPerUnit,
                        String ingredientId) {
        }
}
