// src/main/java/com/restrosync/backend/controller/OrderController.java
package com.restrosync.backend.controller;

import com.restrosync.backend.model.*;
import com.restrosync.backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        // AUTO DEDUCT INVENTORY WHEN ORDER IS SENT
        private void deductInventory(Order order) {
                for (Object itemObj : order.items()) {
                        try {
                                String menuItemId = (String) itemObj.getClass().getMethod("menuItemId").invoke(itemObj);
                                Number qtyNum = (Number) itemObj.getClass().getMethod("qty").invoke(itemObj);
                                double qty = qtyNum != null ? qtyNum.doubleValue() : 0.0;

                                menuRepository.findById(menuItemId).ifPresent(menuItem -> {
                                        if (menuItem.recipe() != null && !menuItem.recipe().isEmpty()) {
                                                menuItem.recipe().forEach(recipe -> {
                                                        inventoryRepository.findById(recipe.ingredientId())
                                                                        .ifPresent(inv -> {
                                                                                double deductAmount = recipe.quantity()
                                                                                                * qty;
                                                                                double newStock = inv.currentStock()
                                                                                                - deductAmount;
                                                                                if (newStock < 0)
                                                                                        newStock = 0.0;

                                                                                InventoryItem updatedInv = inv
                                                                                                .withStock(newStock);
                                                                                inventoryRepository.save(updatedInv);

                                                                                if (newStock <= inv.lowStockAlert()) {
                                                                                        System.out.println(
                                                                                                        "LOW STOCK ALERT: "
                                                                                                                        + inv.name()
                                                                                                                        +
                                                                                                                        " only "
                                                                                                                        + newStock
                                                                                                                        + " "
                                                                                                                        + inv.unit()
                                                                                                                        + " left!");
                                                                                }
                                                                        });
                                                });
                                        }
                                });
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                }
        }

        @PostMapping
        public ResponseEntity<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
                int nextOrderNo = orderRepository.findAll().stream()
                                .filter(o -> o.createdAt() != null
                                                && o.createdAt().toLocalDate().equals(LocalDate.now()))
                                .mapToInt(o -> o.orderNo() != null ? o.orderNo() : 0)
                                .max().orElse(0) + 1;

                String tableId = null;
                if (request.tableNumber() != null && !request.tableNumber().trim().isEmpty()) {
                        tableId = tableRepository.findByNumber(request.tableNumber())
                                        .map(Table::id)
                                        .orElse(null);
                }

                List<Order.OrderItem> orderItems = request.items().stream()
                                .map(i -> new Order.OrderItem(i.menuItemId(), i.name(), i.price(), i.qty()))
                                .toList();

                Order order = new Order(
                                null, nextOrderNo, tableId, request.source(),
                                orderItems, request.total(), "pending",
                                LocalDateTime.now(), LocalDateTime.now());

                Order saved = orderRepository.save(order);
                deductInventory(saved);

                if (tableId != null) {
                        tableRepository.findById(tableId).ifPresent(t -> tableRepository.save(new Table(t.id(),
                                        t.number(), t.chairs(), "occupied", saved.id(), t.x(), t.y())));
                }

                Map<String, Object> res = new HashMap<>();
                res.put("order", saved);
                res.put("orderNo", saved.orderNo());
                return ResponseEntity.ok(res);
        }

        @GetMapping
        public List<Order> getAllOrders() {
                return orderRepository.findAll(
                                org.springframework.data.domain.Sort
                                                .by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        }

        @GetMapping("/kds")
        public List<Order> getCurrentOrders() {
                return orderRepository.findAll().stream()
                                .filter(o -> Set.of("pending", "preparing", "ready").contains(o.status()))
                                .sorted(Comparator.comparing(Order::createdAt).reversed())
                                .toList();
        }

        @PutMapping("/{id}/status")
        public ResponseEntity<Order> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
                String status = body.get("status");
                return orderRepository.findById(id)
                                .map(order -> {
                                        Order updated = new Order(order.id(), order.orderNo(), order.tableId(),
                                                        order.source(),
                                                        order.items(), order.total(), status, order.createdAt(),
                                                        LocalDateTime.now());
                                        return ResponseEntity.ok(orderRepository.save(updated));
                                })
                                .orElse(ResponseEntity.notFound().build());
        }

        @PutMapping("/{id}/pay")
        public ResponseEntity<Order> payOrder(@PathVariable String id) {
                return orderRepository.findById(id)
                                .map(order -> {
                                        Order paid = new Order(order.id(), order.orderNo(), order.tableId(),
                                                        order.source(),
                                                        order.items(), order.total(), "paid", order.createdAt(),
                                                        LocalDateTime.now());
                                        Order saved = orderRepository.save(paid);

                                        if (order.tableId() != null) {
                                                tableRepository.findById(order.tableId()).ifPresent(
                                                                table -> tableRepository.save(new Table(table.id(),
                                                                                table.number(), table.chairs(),
                                                                                "available", null, table.x(),
                                                                                table.y())));
                                        }
                                        return ResponseEntity.ok(saved);
                                })
                                .orElse(ResponseEntity.notFound().build());
        }
}