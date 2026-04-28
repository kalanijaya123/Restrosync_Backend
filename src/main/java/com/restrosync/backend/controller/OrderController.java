// src/main/java/com/restrosync/backend/controller/OrderController.java
package com.restrosync.backend.controller;

import com.restrosync.backend.dto.OrderResponseDto;
import com.restrosync.backend.model.CreateOrderRequest;
import com.restrosync.backend.model.AddItemsRequest;
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
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
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
        public List<OrderResponseDto> getAllOrders() {
                return orderService.listAllOrders();
        }

        @GetMapping("/kds")
        public List<OrderResponseDto> getKdsOrders() {
                return orderService.listKdsOrders();
        }

        @GetMapping("/recent")
        public List<OrderResponseDto> getRecentOrders() {
                return orderService.getRecentOrders();
        }

        @PutMapping("/{id}/status")
        public ResponseEntity<OrderResponseDto> updateStatus(@PathVariable String id,
                        @RequestBody Map<String, String> body) {
                String newStatus = body.get("status");
                if (!java.util.Set
                                .of("payment_pending", "paid_awaiting_kitchen", "pending", "preparing", "ready",
                                                "served", "cancelled")
                                .contains(newStatus)) {
                        return ResponseEntity.badRequest().build();
                }

                OrderResponseDto updated = orderService.updateStatus(id, newStatus);
                return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
        }

        @PutMapping("/{id}/send-to-kitchen")
        public ResponseEntity<OrderResponseDto> sendToKitchen(@PathVariable String id) {
                try {
                        OrderResponseDto sent = orderService.sendToKitchen(id);
                        return sent == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(sent);
                } catch (RuntimeException e) {
                        return ResponseEntity.badRequest().body(null);
                }
        }

        @PutMapping("/{id}/pay")
        public ResponseEntity<OrderResponseDto> payOrder(@PathVariable String id) {
                OrderResponseDto saved = orderService.payOrder(id);
                return saved == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(saved);
        }

        @PutMapping("/{id}/add-items")
        public ResponseEntity<OrderResponseDto> addItemsToOrder(
                        @PathVariable String id,
                        @RequestBody AddItemsRequest request) {
                try {
                        if (request == null || request.items() == null || request.items().isEmpty()) {
                                return ResponseEntity.badRequest().body(null);
                        }
                        OrderResponseDto updated = orderService.addItemsToOrder(id, request);
                        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
                } catch (RuntimeException e) {
                        return ResponseEntity.badRequest().body(null);
                }
        }
}