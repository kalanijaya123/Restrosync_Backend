package com.restrosync.backend.mapper;

import com.restrosync.backend.dto.OrderCreateDto;
import com.restrosync.backend.dto.OrderResponseDto;
import com.restrosync.backend.model.Order;

import java.time.LocalDateTime;

public class OrderMapper {

    public static Order toEntity(OrderCreateDto dto) {
        return new Order(
                null,
                dto.orderNo(),
                dto.tableId(),
                dto.source(),
                dto.items() != null ? dto.items().stream()
                        .map(i -> new Order.OrderItem(
                                i.menuItemId(),
                                i.menuItemName(),
                                i.sizeName(),
                                i.basePrice(),
                                i.qty(),
                                i.extras() != null ? i.extras().stream()
                                        .map(e -> new Order.SelectedExtra(
                                                e.extraId(),
                                                e.name(),
                                                e.price(),
                                                e.qty(),
                                                e.quantityPerUnit(),
                                                e.ingredientId()))
                                        .toList() : null,
                                i.chickenUsed(),
                                i.riceUsed(),
                                i.cheeseUsed(),
                                i.totalIngredientCost()))
                        .toList() : null,
                dto.total(),
                dto.status(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                dto.servedAt(),
                dto.paymentStatus(),
                dto.customerName(),
                dto.customerPhone(),
                dto.notes(),
                dto.waiterName(),
                dto.kotToken());
    }

    public static OrderResponseDto toResponseDto(Order order) {
        return OrderResponseDto.builder()
                .id(order.id())
                .orderNo(order.orderNo())
                .tableId(order.tableId())
                .source(order.source())
                .items(order.items() != null ? order.items().stream()
                        .map(i -> OrderResponseDto.OrderItemDto.builder()
                                .menuItemId(i.menuItemId())
                                .menuItemName(i.menuItemName())
                                .sizeName(i.sizeName())
                                .basePrice(i.basePrice())
                                .qty(i.qty())
                                .extras(i.extras() != null ? i.extras().stream()
                                        .map(e -> OrderResponseDto.SelectedExtraDto.builder()
                                                .extraId(e.extraId())
                                                .name(e.name())
                                                .price(e.price())
                                                .qty(e.qty())
                                                .quantityPerUnit(e.quantityPerUnit())
                                                .ingredientId(e.ingredientId())
                                                .build())
                                        .toList() : null)
                                .chickenUsed(i.chickenUsed())
                                .riceUsed(i.riceUsed())
                                .cheeseUsed(i.cheeseUsed())
                                .totalIngredientCost(i.totalIngredientCost())
                                .build())
                        .toList() : null)
                .total(order.total())
                .status(order.status())
                .createdAt(order.createdAt())
                .updatedAt(order.updatedAt())
                .servedAt(order.servedAt())
                .paymentStatus(order.paymentStatus())
                .customerName(order.customerName())
                .customerPhone(order.customerPhone())
                .notes(order.notes())
                .waiterName(order.waiterName())
                .kotToken(order.kotToken())
                .build();
    }
}
