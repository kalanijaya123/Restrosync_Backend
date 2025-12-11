// src/main/java/com/restrosync/backend/controller/OrderController.java
package com.restrosync.backend.controller;

import com.restrosync.backend.model.CreateOrderRequest;
import com.restrosync.backend.model.Order;
import com.restrosync.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

        private final OrderService orderService;

        @PostMapping
        public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
                if (request == null)
                        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));

                Map<String, Object> res = orderService.createOrder(request);
                return ResponseEntity.ok(res);
        }

        @GetMapping
        public List<Order> getAllOrders() {
                return orderService.listAllOrders();
        }

        @GetMapping("/kds")
        public List<Order> getKdsOrders() {
                return orderService.listKdsOrders();
        }

        @PutMapping("/{id}/status")
        public ResponseEntity<Order> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
                String newStatus = body.get("status");
                if (!java.util.Set
                                .of("payment_pending", "paid_awaiting_kitchen", "pending", "preparing", "ready",
                                                "served", "cancelled")
                                .contains(newStatus)) {
                        return ResponseEntity.badRequest().build();
                }

                Order updated = orderService.updateStatus(id, newStatus);
                return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
        }

        @PutMapping("/{id}/send-to-kitchen")
        public ResponseEntity<Order> sendToKitchen(@PathVariable String id) {
                try {
                        Order sent = orderService.sendToKitchen(id);
                        return sent == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(sent);
                } catch (RuntimeException e) {
                        return ResponseEntity.badRequest().body(null);
                }
        }

        @PutMapping("/{id}/pay")
        public ResponseEntity<Order> payOrder(@PathVariable String id) {
                Order saved = orderService.payOrder(id);
                return saved == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(saved);
        }
}