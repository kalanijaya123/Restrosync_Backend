package com.restrosync.backend.mapper;

import com.restrosync.backend.dto.MenuItemCreateDto;
import com.restrosync.backend.dto.MenuItemResponseDto;
import com.restrosync.backend.model.MenuItem;

import java.util.stream.Collectors;

public class MenuItemMapper {

        public static MenuItem toEntity(MenuItemCreateDto dto) {
                return new MenuItem(
                                null,
                                dto.name(),
                                dto.category(),
                                dto.price(),
                                dto.sizes() != null ? dto.sizes().stream()
                                                .map(s -> new MenuItem.Size(s.name(), s.price()))
                                                .toList() : null,
                                dto.available(),
                                dto.mediaUrl(),
                                dto.recipe() != null ? dto.recipe().stream()
                                                .map(r -> new MenuItem.RecipeItem(r.ingredientId(), r.ingredientName(),
                                                                r.quantities()))
                                                .toList() : null,
                                dto.extras() != null ? dto.extras().stream()
                                                .map(e -> new MenuItem.ExtraItem(e.id(), e.name(), e.price(),
                                                                e.quantityPerUnit(),
                                                                e.ingredientId()))
                                                .toList() : null,
                                dto.mealPeriods());
        }

        public static MenuItemResponseDto toResponseDto(MenuItem item) {
                return MenuItemResponseDto.builder()
                                .id(item.id())
                                .name(item.name())
                                .category(item.category())
                                .price(item.price())
                                .sizes(item.sizes() != null ? item.sizes().stream()
                                                .map(s -> new MenuItemResponseDto.SizeDto(s.name(), s.price()))
                                                .toList() : null)
                                .available(item.available())
                                .mediaUrl(item.mediaUrl())
                                .recipe(item.recipe() != null ? item.recipe().stream()
                                                .map(r -> new MenuItemResponseDto.RecipeItemDto(r.ingredientId(),
                                                                r.ingredientName(),
                                                                r.quantities()))
                                                .toList() : null)
                                .extras(item.extras() != null ? item.extras().stream()
                                                .map(e -> new MenuItemResponseDto.ExtraItemDto(e.id(), e.name(),
                                                                e.price(),
                                                                e.quantityPerUnit(), e.ingredientId()))
                                                .toList() : null)
                                .mealPeriods(item.mealPeriods())
                                .build();
        }
}
