package com.restrosync.backend.controller;

import com.restrosync.backend.model.Order;
import com.restrosync.backend.model.Table;
import com.restrosync.backend.repository.OrderRepository;
import com.restrosync.backend.repository.TableRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
                "totalRevenue", totalRevenue,
                "activeTables", activeTables);
        return ResponseEntity.ok(stats);
    }

    // THIRD-PARTY ORDERS
    @GetMapping("/thirdparty")
    public List<Order> getThirdPartyOrders() {
        // Simulate real 3rd-party orders (UberEats, DoorDash)
        return List.of(
                new Order(null, "UberEats", List.of(
                        new Order.OrderItem("Burger", 1, 12.99),
                        new Order.OrderItem("Fries", 1, 4.99)), 17.98, "pending", LocalDateTime.now(),
                        LocalDateTime.now()),
                new Order(null, "DoorDash", List.of(
                        new Order.OrderItem("Pizza", 2, 15.99)), 31.98, "preparing",
                        LocalDateTime.now().minusMinutes(5), LocalDateTime.now()));
    }

    @GetMapping("/current")
    public List<Order> getCurrentOrders() {
        return orderRepository.findAll().stream()
                .filter(o -> Set.of("pending", "preparing", "ready").contains(o.status()))
                .toList();
    }

    @GetMapping("/history")
    public List<Order> getOrderHistory() {
        return orderRepository.findAll().stream()
                .filter(o -> "paid".equals(o.status()))
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();
    }

    @GetMapping("/current/total")
    public ResponseEntity<Map<String, Double>> getCurrentTotal() {
        double total = orderRepository.findAll().stream()
                .filter(o -> Set.of("pending", "preparing", "ready").contains(o.status()))
                .mapToDouble(Order::total)
                .sum();

        return ResponseEntity.ok(Map.of("total", total));
    }

    @GetMapping("/kds")
    public List<Order> getKDS() {
        return orderRepository.findAll().stream()
                .filter(o -> Set.of("pending", "preparing").contains(o.status()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order newOrder = new Order(
                null,
                order.tableId(),
                order.items(),
                order.total(),
                "pending",
                LocalDateTime.now(),
                LocalDateTime.now());
        Order saved = orderRepository.save(newOrder);

        tableRepository.findById(order.tableId())
                .ifPresent(t -> tableRepository.save(new Table(t.id(), t.number(), "occupied", saved.id())));

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        return orderRepository.findById(id)
                .map(order -> {
                    Order updated = new Order(
                            order.id(), order.tableId(), order.items(), order.total(),
                            body.get("status"), order.createdAt(), LocalDateTime.now());
                    return ResponseEntity.ok(orderRepository.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<Order> payOrder(@PathVariable String id) {
        return orderRepository.findById(id)
                .map(order -> {
                    Order paid = new Order(
                            order.id(), order.tableId(), order.items(), order.total(),
                            "paid", order.createdAt(), LocalDateTime.now());
                    Order saved = orderRepository.save(paid);

                    tableRepository.findById(order.tableId())
                            .ifPresent(t -> tableRepository.save(new Table(t.id(), t.number(), "dirty", null)));

                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
}