package com.restrosync.backend.service;

import com.restrosync.backend.model.*;
import com.restrosync.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final TableRepository tableRepository;
    private final MenuRepository menuRepository;
    private final InventoryRepository inventoryRepository;

    private int nextOrderNo() {
        LocalDate today = LocalDate.now();
        return orderRepository.findAll().stream()
                .filter(o -> o.createdAt() != null && o.createdAt().toLocalDate().equals(today))
                .mapToInt(o -> o.orderNo() != null ? o.orderNo() : 0)
                .max().orElse(0) + 1;
    }

    // Deduct inventory using per-size recipe quantities
    private void deductInventory(Order order) {
        order.items().forEach(orderItem -> {
            menuRepository.findById(orderItem.menuItemId()).ifPresent(menuItem -> {
                if (menuItem.recipe() != null) {
                    menuItem.recipe().forEach(recipe -> {
                        double qtyPerUnit = 0.0;
                        var quantitiesMap = recipe.quantities();
                        if (quantitiesMap != null && !quantitiesMap.isEmpty()) {
                            String size = orderItem.sizeName();
                            qtyPerUnit = quantitiesMap.getOrDefault(size,
                                    quantitiesMap.getOrDefault("Regular",
                                            quantitiesMap.values().stream().findFirst().orElse(0.0)));
                        }
                        double totalDeduct = qtyPerUnit * orderItem.qty();
                        inventoryRepository.findById(recipe.ingredientId()).ifPresent(inv -> {
                            double newStock = Math.max(0, inv.currentStock() - totalDeduct);
                            inventoryRepository.save(inv.withStock(newStock));
                            if (newStock <= inv.lowStockAlert()) {
                                log.warn("LOW STOCK ALERT: {} → {} {}", inv.name(), newStock, inv.unit());
                            }
                        });
                    });
                }

                if (menuItem.extras() != null && orderItem.extras() != null) {
                    orderItem.extras().forEach(selectedExtra -> {
                        menuItem.extras().stream()
                                .filter(extra -> extra.id().equals(selectedExtra.extraId()))
                                .findFirst()
                                .ifPresent(extra -> {
                                    double deductExtra = extra.quantityPerUnit() * selectedExtra.qty()
                                            * orderItem.qty();
                                    inventoryRepository.findById(extra.ingredientId()).ifPresent(inv -> {
                                        double newStock = Math.max(0, inv.currentStock() - deductExtra);
                                        inventoryRepository.save(inv.withStock(newStock));
                                        if (newStock <= inv.lowStockAlert()) {
                                            log.warn("LOW STOCK (EXTRA): {} → {}", inv.name(), newStock);
                                        }
                                    });
                                });
                    });
                }
            });
        });
    }

    public Map<String, Object> createOrder(CreateOrderRequest request) {
        int orderNo = nextOrderNo();

        String tableId = null;
        if (request.tableNumber() != null && !request.tableNumber().trim().isEmpty()) {
            tableId = tableRepository.findByNumber(request.tableNumber()).map(Table::id).orElse(null);
        }

        List<Order.OrderItem> orderItems = new ArrayList<>();
        for (CreateOrderRequest.OrderItemReq i : request.items()) {
            MenuItem menu = menuRepository.findById(i.menuItemId()).orElse(null);
            String itemName = menu != null ? menu.name() : "Unknown";
            List<Order.SelectedExtra> extras = i.extras() == null ? List.of() : i.extras().stream().map(se -> {
                MenuItem.ExtraItem extra = menu != null
                        ? menu.extras().stream().filter(e -> e.id().equals(se.extraId())).findFirst().orElse(null)
                        : null;
                return new Order.SelectedExtra(se.extraId(), extra != null ? extra.name() : "Unknown",
                        extra != null ? extra.price() : 0.0, se.qty(), extra != null ? extra.quantityPerUnit() : 0.0,
                        extra != null ? extra.ingredientId() : null);
            }).toList();

            orderItems.add(new Order.OrderItem(i.menuItemId(), itemName, i.sizeName(), i.price(), i.qty(), extras, 0.0,
                    0.0, 0.0, 0.0));
        }

        Order order = new Order(null, orderNo, tableId, request.source(), orderItems, request.total(), "pending",
                LocalDateTime.now(), LocalDateTime.now(), null, "pending", request.customerName(),
                request.customerPhone(), request.notes(), request.waiterName(), null);

        Order saved = orderRepository.save(order);
        deductInventory(saved);

        if (tableId != null) {
            tableRepository.findById(tableId).ifPresent(t -> tableRepository
                    .save(new Table(t.id(), t.number(), t.chairs(), t.chairs(), "occupied", saved.id(), t.x(), t.y())));
        }

        Map<String, Object> res = new HashMap<>();
        res.put("order", saved);
        res.put("orderNo", saved.orderNo());
        return res;
    }

    public List<Order> listAllOrders() {
        return orderRepository.findAll().stream().sorted(Comparator.comparing(Order::createdAt).reversed()).toList();
    }

    public List<Order> listKdsOrders() {
        return orderRepository.findAll().stream()
                .filter(o -> Set.of("pending", "preparing", "ready").contains(o.status()))
                .sorted(Comparator.comparing(Order::createdAt))
                .toList();
    }

    public Order updateStatus(String id, String newStatus) {
        if (!Set.of("pending", "preparing", "ready", "served", "cancelled").contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status");
        }
        return orderRepository.findById(id).map(order -> {
            Order updated = new Order(
                    order.id(), order.orderNo(), order.tableId(), order.source(),
                    order.items(), order.total(), newStatus,
                    order.createdAt(), LocalDateTime.now(),
                    newStatus.equals("served") ? LocalDateTime.now() : order.servedAt(),
                    order.paymentStatus(), order.customerName(), order.customerPhone(),
                    order.notes(), order.waiterName(), order.kotToken());
            return orderRepository.save(updated);
        }).orElse(null);
    }

    public Order payOrder(String id) {
        return orderRepository.findById(id).map(order -> {
            Order paid = new Order(
                    order.id(), order.orderNo(), order.tableId(), order.source(),
                    order.items(), order.total(), "paid",
                    order.createdAt(), LocalDateTime.now(),
                    order.servedAt(), "paid",
                    order.customerName(), order.customerPhone(),
                    order.notes(), order.waiterName(), order.kotToken());
            Order saved = orderRepository.save(paid);

            // Free table
            if (order.tableId() != null) {
                tableRepository.findById(order.tableId())
                        .ifPresent(t -> tableRepository.save(
                                new Table(t.id(), t.number(), t.chairs(), 0, "available",
                                        null, t.x(), t.y())));
            }
            return saved;
        }).orElse(null);
    }
}
