package com.restrosync.backend.controller;

import com.restrosync.backend.model.OnlineOrder;
import com.restrosync.backend.service.OnlineOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/online-orders")
@RequiredArgsConstructor
@Slf4j
public class OnlineOrderController {

    private final OnlineOrderService onlineOrderService;

    /**
     * Create new online order
     */
    @PostMapping
    public ResponseEntity<OnlineOrder> createOrder(@RequestBody OnlineOrder order) {
        log.info("Received online order from: {}", order.customerName());
        return ResponseEntity.ok(onlineOrderService.createOnlineOrder(order));
    }

    /**
     * Track order by token (customer facing)
     */
    @GetMapping("/track/{token}")
    public ResponseEntity<OnlineOrder> trackOrder(@PathVariable String token) {
        Optional<OnlineOrder> order = onlineOrderService.getOrderByTrackingToken(token);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get pending orders for staff confirmation
     */
    @GetMapping("/pending")
    public ResponseEntity<List<OnlineOrder>> getPendingOrders() {
        return ResponseEntity.ok(onlineOrderService.getPendingOrders());
    }

    /**
     * Get all online orders for staff views
     */
    @GetMapping
    public ResponseEntity<List<OnlineOrder>> getAllOrders() {
        return ResponseEntity.ok(onlineOrderService.getAllOrders());
    }

    /**
     * Get confirmed orders ready for kitchen
     */
    @GetMapping("/confirmed")
    public ResponseEntity<List<OnlineOrder>> getConfirmedOrders() {
        return ResponseEntity.ok(onlineOrderService.getConfirmedOrders());
    }

    /**
     * Update order status
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<String> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam String status) {
        onlineOrderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok("Order status updated to: " + status);
    }

    /**
     * Accept an online order and immediately push it into the kitchen queue.
     */
    @PutMapping("/{orderId}/accept")
    public ResponseEntity<String> acceptOrder(@PathVariable String orderId) {
        onlineOrderService.acceptOrderForKitchen(orderId);
        return ResponseEntity.ok("Order accepted and sent to kitchen");
    }
}
