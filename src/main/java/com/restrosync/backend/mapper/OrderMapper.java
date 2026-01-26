package com.restrosync.backend.mapper;

import com.restrosync.backend.dto.OrderCreateDto;
import com.restrosync.backend.dto.OrderResponseDto;
import com.restrosync.backend.model.MenuItem;
import com.restrosync.backend.model.Order;
import com.restrosync.backend.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OrderMapper {

        private final MenuRepository menuRepository;

        public Order toEntity(OrderCreateDto dto) {
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
                                0.0, // amountPaid
                                0.0, // changeGiven
                                null, // paymentMethod
                                null, // paymentTime
                                LocalDateTime.now(), // createdAt
                                LocalDateTime.now(), // updatedAt
                                dto.servedAt(),
                                dto.paymentStatus(),
                                dto.customerName(),
                                dto.customerPhone(),
                                dto.notes(),
                                dto.waiterName(),
                                dto.kotToken());
        }

        public OrderResponseDto toResponseDto(Order order) {
                return OrderResponseDto.builder()
                                .id(order.id())
                                .orderNo(order.orderNo())
                                .tableId(order.tableId())
                                .source(order.source())
                                .items(order.items() != null ? order.items().stream()
                                                .map(i -> {
                                                        // Fetch menu item to get mediaUrl
                                                        String mediaUrl = null;
                                                        if (i.menuItemId() != null) {
                                                                MenuItem menuItem = menuRepository
                                                                                .findById(i.menuItemId()).orElse(null);
                                                                if (menuItem != null) {
                                                                        mediaUrl = menuItem.mediaUrl();
                                                                }
                                                        }

                                                        return OrderResponseDto.OrderItemDto.builder()
                                                                        .menuItemId(i.menuItemId())
                                                                        .menuItemName(i.menuItemName())
                                                                        .mediaUrl(mediaUrl)
                                                                        .sizeName(i.sizeName())
                                                                        .basePrice(i.basePrice())
                                                                        .qty(i.qty())
                                                                        .extras(i.extras() != null ? i.extras().stream()
                                                                                        .map(e -> OrderResponseDto.SelectedExtraDto
                                                                                                        .builder()
                                                                                                        .extraId(e.extraId())
                                                                                                        .name(e.name())
                                                                                                        .price(e.price())
                                                                                                        .qty(e.qty())
                                                                                                        .quantityPerUnit(
                                                                                                                        e.quantityPerUnit())
                                                                                                        .ingredientId(e.ingredientId())
                                                                                                        .build())
                                                                                        .toList() : null)
                                                                        .chickenUsed(i.chickenUsed())
                                                                        .riceUsed(i.riceUsed())
                                                                        .cheeseUsed(i.cheeseUsed())
                                                                        .totalIngredientCost(i.totalIngredientCost())
                                                                        .build();
                                                })
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
