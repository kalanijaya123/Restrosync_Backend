// src/main/java/com/restrosync/backend/controller/OrderController.java
package com.restrosync.backend.controller;

import com.restrosync.backend.model.*;
import com.restrosync.backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {

        private final OrderRepository orderRepository;
        private final TableRepository tableRepository;
        private final MenuRepository menuRepository;
        private final InventoryRepository inventoryRepository;

        public OrderController(OrderRepository orderRepository,
                        TableRepository tableRepository,
                        MenuRepository menuRepository,
                        InventoryRepository inventoryRepository) {
                this.orderRepository = orderRepository;
                this.tableRepository = tableRepository;
                this.menuRepository = menuRepository;
                this.inventoryRepository = inventoryRepository;
        }

        // AUTO DEDUCT INVENTORY WHEN ORDER IS SENT TO KITCHEN
        private void deductInventory(Order order) {
                order.items().forEach(orderItem -> {
                        menuRepository.findById(orderItem.menuItemId()).ifPresent(menuItem -> {

                                // 1. DEDUCT MAIN RECIPE (PER-SIZE QUANTITY)
                                if (menuItem.recipe() != null) {
                                        menuItem.recipe().forEach(recipe -> {
                                                double qtyPerUnit = 0.0;
                                                var quantitiesMap = recipe.quantities();

                                                if (quantitiesMap != null && !quantitiesMap.isEmpty()) {
                                                        String size = orderItem.sizeName();
                                                        qtyPerUnit = quantitiesMap.getOrDefault(size,
                                                                        quantitiesMap.getOrDefault("Regular",
                                                                                        quantitiesMap.values().stream()
                                                                                                        .findFirst()
                                                                                                        .orElse(0.0)));
                                                }

                                                double totalDeduct = qtyPerUnit * orderItem.qty();

                                                inventoryRepository.findById(recipe.ingredientId()).ifPresent(inv -> {
                                                        double newStock = Math.max(0, inv.currentStock() - totalDeduct);
                                                        inventoryRepository.save(inv.withStock(newStock));

                                                        if (newStock <= inv.lowStockAlert()) {
                                                                System.out.println("LOW STOCK ALERT: " + inv.name()
                                                                                + " → " + newStock + " " + inv.unit());
                                                        }
                                                });
                                        });
                                }

                                // 2. DEDUCT EXTRAS (Extra Cheese ×3 = 0.15kg cheese)
                                if (menuItem.extras() != null && orderItem.extras() != null) {
                                        orderItem.extras().forEach(selectedExtra -> {
                                                menuItem.extras().stream()
                                                                .filter(extra -> extra.id()
                                                                                .equals(selectedExtra.extraId()))
                                                                .findFirst()
                                                                .ifPresent(extra -> {
                                                                        double deductExtra = extra.quantityPerUnit()
                                                                                        * selectedExtra.qty()
                                                                                        * orderItem.qty();

                                                                        inventoryRepository
                                                                                        .findById(extra.ingredientId())
                                                                                        .ifPresent(inv -> {
                                                                                                double newStock = Math
                                                                                                                .max(0, inv.currentStock()
                                                                                                                                - deductExtra);
                                                                                                inventoryRepository
                                                                                                                .save(inv.withStock(
                                                                                                                                newStock));

                                                                                                if (newStock <= inv
                                                                                                                .lowStockAlert()) {
                                                                                                        System.out.println(
                                                                                                                        "LOW STOCK (EXTRA): "
                                                                                                                                        + inv.name()
                                                                                                                                        + " → "
                                                                                                                                        + newStock);
                                                                                                }
                                                                                        });
                                                                });
                                        });
                                }
                        });
                });
        }

        // Generate KOT Token like KOT-001, KOT-002
        private String generateKotToken() {
                int todayCount = orderRepository.findAll().stream()
                                .filter(o -> o.createdAt() != null
                                                && o.createdAt().toLocalDate().equals(LocalDate.now()))
                                .toList().size() + 1;
                return "KOT-" + String.format("%03d", todayCount);
        }

        @PostMapping
        public ResponseEntity<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
                try {
                        // Generate Order Number (daily reset)
                        int nextOrderNo = orderRepository.findAll().stream()
                                        .filter(o -> o.createdAt() != null
                                                        && o.createdAt().toLocalDate().equals(LocalDate.now()))
                                        .mapToInt(o -> o.orderNo() != null ? o.orderNo() : 0)
                                        .max().orElse(0) + 1;

                        // Find Table ID
                        String tableId = null;
                        if (request.tableNumber() != null && !request.tableNumber().trim().isEmpty()) {
                                tableId = tableRepository.findByNumber(request.tableNumber())
                                                .map(Table::id)
                                                .orElse(null);
                        }

                        // Build Order Items with full data
                        List<Order.OrderItem> orderItems = new ArrayList<>();
                        for (CreateOrderRequest.OrderItemReq itemReq : request.items()) {
                                MenuItem menuItem = menuRepository.findById(itemReq.menuItemId()).orElse(null);
                                String itemName = menuItem != null ? menuItem.name() : "Unknown Item";

                                List<Order.SelectedExtra> extras = itemReq.extras() == null ? List.of()
                                                : itemReq.extras().stream()
                                                                .map(se -> {
                                                                        MenuItem.ExtraItem extra = menuItem != null
                                                                                        ? menuItem.extras().stream()
                                                                                                        .filter(e -> e.id()
                                                                                                                        .equals(se.extraId()))
                                                                                                        .findFirst()
                                                                                                        .orElse(null)
                                                                                        : null;

                                                                        return new Order.SelectedExtra(
                                                                                        se.extraId(),
                                                                                        extra != null ? extra.name()
                                                                                                        : "Unknown Extra",
                                                                                        extra != null ? extra.price()
                                                                                                        : 0.0,
                                                                                        se.qty(),
                                                                                        extra != null ? extra
                                                                                                        .quantityPerUnit()
                                                                                                        : 0.0,
                                                                                        extra != null ? extra
                                                                                                        .ingredientId()
                                                                                                        : null);
                                                                })
                                                                .toList();

                                orderItems.add(new Order.OrderItem(
                                                itemReq.menuItemId(),
                                                itemName,
                                                itemReq.sizeName(),
                                                itemReq.price(), // basePrice
                                                itemReq.qty(), // qty
                                                extras,
                                                0.0, // chickenUsed (computed later if desired)
                                                0.0, // riceUsed
                                                0.0, // cheeseUsed
                                                0.0 // totalIngredientCost
                                ));
                        }

                        // Create final Order
                        Order order = new Order(
                                        null,
                                        nextOrderNo,
                                        tableId,
                                        request.source(),
                                        orderItems,
                                        request.total(),
                                        "pending",
                                        LocalDateTime.now(),
                                        LocalDateTime.now(),
                                        null, // servedAt
                                        "pending", // paymentStatus
                                        request.customerName() != null ? request.customerName() : "Walk-in",
                                        request.customerPhone(),
                                        request.notes(),
                                        request.waiterName() != null ? request.waiterName() : "Staff",
                                        generateKotToken());

                        Order saved = orderRepository.save(order);

                        // DEDUCT INVENTORY AFTER SAVING
                        deductInventory(saved);

                        // Update table status
                        if (tableId != null) {
                                tableRepository.findById(tableId).ifPresent(t -> tableRepository.save(new Table(t.id(),
                                                t.number(), t.chairs(), "occupied", saved.id(), t.x(), t.y())));
                        }

                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("order", saved);
                        response.put("kotToken", saved.kotToken());
                        response.put("orderNo", saved.orderNo());

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
                }
        }

        @GetMapping
        public List<Order> getAllOrders() {
                return orderRepository.findAll()
                                .stream()
                                .sorted(Comparator.comparing(Order::createdAt).reversed())
                                .toList();
        }

        @GetMapping("/kds")
        public List<Order> getKdsOrders() {
                return orderRepository.findAll().stream()
                                .filter(o -> Set.of("pending", "preparing", "ready").contains(o.status()))
                                .sorted(Comparator.comparing(Order::createdAt))
                                .toList();
        }

        @PutMapping("/{id}/status")
        public ResponseEntity<Order> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
                String newStatus = body.get("status");
                if (!Set.of("pending", "preparing", "ready", "served", "cancelled").contains(newStatus)) {
                        return ResponseEntity.badRequest().build();
                }

                return orderRepository.findById(id)
                                .map(order -> {
                                        Order updated = new Order(
                                                        order.id(), order.orderNo(), order.tableId(), order.source(),
                                                        order.items(), order.total(), newStatus,
                                                        order.createdAt(), LocalDateTime.now(),
                                                        newStatus.equals("served") ? LocalDateTime.now()
                                                                        : order.servedAt(),
                                                        order.paymentStatus(), order.customerName(),
                                                        order.customerPhone(),
                                                        order.notes(), order.waiterName(), order.kotToken());
                                        return ResponseEntity.ok(orderRepository.save(updated));
                                })
                                .orElse(ResponseEntity.notFound().build());
        }

        @PutMapping("/{id}/pay")
        public ResponseEntity<Order> payOrder(@PathVariable String id) {
                return orderRepository.findById(id)
                                .map(order -> {
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
                                                                .ifPresent(t -> tableRepository.save(new Table(t.id(),
                                                                                t.number(), t.chairs(), "available",
                                                                                null, t.x(), t.y())));
                                        }
                                        return ResponseEntity.ok(saved);
                                })
                                .orElse(ResponseEntity.notFound().build());
        }
}