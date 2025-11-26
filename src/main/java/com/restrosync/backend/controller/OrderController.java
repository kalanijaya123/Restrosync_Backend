package com.restrosync.backend.controller;

import com.restrosync.backend.model.Order;
import com.restrosync.backend.model.Table;
import com.restrosync.backend.repository.OrderRepository;
import com.restrosync.backend.repository.TableRepository;
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

        public OrderController(OrderRepository orderRepository, TableRepository tableRepository) {
                this.orderRepository = orderRepository;
                this.tableRepository = tableRepository;
        }

        // DASHBOARD STATS
        @GetMapping("/dashboard/stats")
        public ResponseEntity<Map<String, Object>> getDashboardStats() {
                long totalOrders = orderRepository.count();
                double totalRevenue = orderRepository.findAll().stream()
                                .filter(o -> "paid".equals(o.status()))
                                .mapToDouble(Order::total)
                                .sum();

                long activeTables = tableRepository.findAll().stream()
                                .filter(t -> "occupied".equals(t.status()))
                                .count();

                Map<String, Object> stats = Map.of(
                                "totalOrders", totalOrders,
                                "totalRevenue", Math.round(totalRevenue * 100.0) / 100.0,
                                "activeTables", activeTables);
                return ResponseEntity.ok(stats);
        }

        // SIMULATED THIRD-PARTY ORDERS
        @GetMapping("/thirdparty")
        public List<Order> getThirdPartyOrders() {
                // Use Arrays.asList for compatibility with older Java versions and avoid
                // referencing Order.OrderItem
                // (which may not be a visible nested type); provide empty item lists for these
                // simulated entries.
                return Arrays.asList(
                                new Order(null, 999, null, "UberEats", Collections.emptyList(), 35.96, "pending",
                                                LocalDateTime.now().minusMinutes(3), LocalDateTime.now()),
                                new Order(null, 998, null, "DoorDash", Collections.emptyList(), 18.99, "preparing",
                                                LocalDateTime.now().minusMinutes(8), LocalDateTime.now()));
        }

        @GetMapping("/current")
        public List<Order> getCurrentOrders() {
                return orderRepository.findAll().stream()
                                .filter(o -> Set.of("pending", "preparing", "ready").contains(o.status()))
                                .sorted(Comparator.comparing(Order::createdAt).reversed())
                                .toList();
        }

        @GetMapping("/history")
        public List<Order> getOrderHistory() {
                return orderRepository.findAll().stream()
                                .filter(o -> "paid".equals(o.status()))
                                .sorted(Comparator.comparing(Order::createdAt).reversed())
                                .toList();
        }

        @GetMapping("/current/total")
        public ResponseEntity<Map<String, Double>> getCurrentTotal() {
                double total = orderRepository.findAll().stream()
                                .filter(o -> Set.of("pending", "preparing", "ready").contains(o.status()))
                                .mapToDouble(Order::total)
                                .sum();
                return ResponseEntity.ok(Map.of("total", Math.round(total * 100.0) / 100.0));
        }

        @GetMapping("/kds")
        public List<Order> getKDSOrders() {
                return orderRepository.findAll().stream()
                                .filter(o -> Set.of("pending", "preparing").contains(o.status()))
                                .sorted(Comparator.comparing(Order::createdAt))
                                .toList();
        }

        // CREATE ORDER WITH DAILY RESETTING ORDER NUMBER
        @PostMapping
        public ResponseEntity<?> createOrder(
                        @RequestBody com.restrosync.backend.model.CreateOrderRequest orderRequest) {
                LocalDate today = LocalDate.now();

                // Get today's highest orderNo
                int nextOrderNo = orderRepository.findAll().stream()
                                .filter(o -> o.createdAt() != null && o.createdAt().toLocalDate().equals(today))
                                .mapToInt(o -> o.orderNo() != null ? o.orderNo() : 0)
                                .max()
                                .orElse(0) + 1;

                // Resolve tableId from provided tableNumber (if any)
                String tableId = null;
                if (orderRequest.tableNumber() != null && !orderRequest.tableNumber().isBlank()) {
                        tableId = tableRepository.findByNumber(orderRequest.tableNumber())
                                        .map(Table::id)
                                        .orElse(null);
                }

                Order newOrder = new Order(
                                null,
                                nextOrderNo,
                                tableId,
                                orderRequest.source(), // "dine-in" or "takeaway"
                                orderRequest.items(),
                                orderRequest.total(),
                                "pending",
                                LocalDateTime.now(),
                                LocalDateTime.now());

                Order savedOrder = orderRepository.save(newOrder);

                Map<String, Object> response = new HashMap<>();
                response.put("order", savedOrder);
                response.put("orderNo", savedOrder.orderNo());

                // ONLY UPDATE TABLE IF we resolved a tableId
                if (tableId != null && !tableId.isBlank()) {
                        tableRepository.findById(tableId)
                                        .ifPresent(table -> {
                                                Table updatedTable = new Table(
                                                                table.id(),
                                                                table.number(),
                                                                table.chairs(),
                                                                "occupied",
                                                                savedOrder.id(),
                                                                table.x(),
                                                                table.y());
                                                tableRepository.save(updatedTable);
                                                response.put("table", updatedTable);
                                        });
                }

                return ResponseEntity.ok(response);
        }

        // UPDATE ORDER STATUS
        @PutMapping("/{id}/status")
        public ResponseEntity<Order> updateOrderStatus(
                        @PathVariable String id,
                        @RequestBody Map<String, String> body) {

                String newStatus = body.get("status");

                return orderRepository.findById(id)
                                .map(order -> {
                                        Order updated = new Order(
                                                        order.id(),
                                                        order.orderNo(),
                                                        order.tableId(),
                                                        order.source(),
                                                        order.items(),
                                                        order.total(),
                                                        newStatus,
                                                        order.createdAt(),
                                                        LocalDateTime.now());
                                        return ResponseEntity.ok(orderRepository.save(updated));
                                })
                                .orElse(ResponseEntity.notFound().build());
        }

        // PAY ORDER → Reset table only if it had one
        @PutMapping("/{id}/pay")
        public ResponseEntity<Order> payOrder(@PathVariable String id) {
                return orderRepository.findById(id)
                                .map(order -> {
                                        Order paidOrder = new Order(
                                                        order.id(),
                                                        order.orderNo(),
                                                        order.tableId(),
                                                        order.source(),
                                                        order.items(),
                                                        order.total(),
                                                        "paid",
                                                        order.createdAt(),
                                                        LocalDateTime.now());
                                        Order saved = orderRepository.save(paidOrder);

                                        // Only free the table if it was assigned
                                        if (order.tableId() != null && !order.tableId().isBlank()) {
                                                tableRepository.findById(order.tableId())
                                                                .ifPresent(table -> {
                                                                        Table updatedTable = new Table(
                                                                                        table.id(),
                                                                                        table.number(),
                                                                                        table.chairs(),
                                                                                        "available",
                                                                                        null,
                                                                                        table.x(),
                                                                                        table.y());
                                                                        tableRepository.save(updatedTable);
                                                                });
                                        }

                                        return ResponseEntity.ok(saved);
                                })
                                .orElse(ResponseEntity.notFound().build());
        }

        // Clear table (after cleaning)
        @PutMapping("/table/{tableId}/clear")
        public ResponseEntity<Void> clearTable(@PathVariable String tableId) {
                tableRepository.findById(tableId)
                                .ifPresent(table -> {
                                        Table cleaned = new Table(
                                                        table.id(),
                                                        table.number(),
                                                        table.chairs(),
                                                        "available",
                                                        null,
                                                        table.x(),
                                                        table.y());
                                        tableRepository.save(cleaned);
                                });
                return ResponseEntity.ok().build();
        }
}