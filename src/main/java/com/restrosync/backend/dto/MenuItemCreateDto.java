package com.restrosync.backend.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record MenuItemCreateDto(
                String name,
                String category,
                Double price,
                List<SizeDto> sizes,
                Boolean available,
                String mediaUrl,
                List<RecipeItemDto> recipe,
                List<ExtraItemDto> extras,
                List<String> mealPeriods) {
        @Builder
        public record SizeDto(String name, Double price) {
        }

        @Builder
        public record RecipeItemDto(
                        String ingredientId,
                        String ingredientName,
                        Map<String, Double> quantities) {
        }

        @Builder
        public record ExtraItemDto(
                        String id,
                        String name,
                        Double price,
                        Double quantityPerUnit,
                        String ingredientId) {
        }
}
