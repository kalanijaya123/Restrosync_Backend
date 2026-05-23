package com.restrosync.backend.service;

import com.restrosync.backend.model.OnlineOrder;
import com.restrosync.backend.model.Order;
import com.restrosync.backend.repository.OnlineOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineOrderService {

    private final OnlineOrderRepository onlineOrderRepository;

    /**
     * Create new online order (Requirement 4.7)
     */
    public OnlineOrder createOnlineOrder(OnlineOrder order) {
        log.info("Creating online order for customer: {}", order.customerName());

        String orderNumber = "ONL-" + System.currentTimeMillis();
        String trackingToken = UUID.randomUUID().toString();

        OnlineOrder newOrder = new OnlineOrder(
                null,
                orderNumber,
                null, // Will be set after customer login
                order.customerName(),
                order.customerPhone(),
                order.customerEmail(),
                order.deliveryAddress(),
                order.deliveryType(),
                order.items(),
                order.subtotal(),
                order.deliveryFee(),
                order.discountAmount(),
                order.tax(),
                order.total(),
                "pending_payment", // Initial status
                "pending", // Payment status
                order.paymentMethod(),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(30), // Expected delivery in 30 mins for now
                null,
                order.notes(),
                trackingToken);

        return onlineOrderRepository.save(newOrder);
    }

    /**
     * Get order by tracking token (for customer tracking)
     */
    public Optional<OnlineOrder> getOrderByTrackingToken(String token) {
        return onlineOrderRepository.findByTrackingToken(token);
    }

    /**
     * Get all online orders for POS/management views
     */
    public List<OnlineOrder> getAllOrders() {
        return onlineOrderRepository.findAll().stream()
                .sorted((a, b) -> b.orderedAt().compareTo(a.orderedAt()))
                .toList();
    }

    /**
     * Update order status
     */
    public void updateOrderStatus(String orderId, String newStatus) {
        onlineOrderRepository.findById(orderId).ifPresent(order -> {
            OnlineOrder updated = new OnlineOrder(
                    order.id(),
                    order.orderNumber(),
                    order.customerId(),
                    order.customerName(),
                    order.customerPhone(),
                    order.customerEmail(),
                    order.deliveryAddress(),
                    order.deliveryType(),
                    order.items(),
                    order.subtotal(),
                    order.deliveryFee(),
                    order.discountAmount(),
                    order.tax(),
                    order.total(),
                    newStatus,
                    order.paymentStatus(),
                    order.paymentMethod(),
                    order.orderedAt(),
                    order.expectedDeliveryAt(),
                    "delivered".equals(newStatus) ? LocalDateTime.now() : order.deliveredAt(),
                    order.notes(),
                    order.trackingToken());
            onlineOrderRepository.save(updated);
            log.info("Online order {} status updated to: {}", orderId, newStatus);
        });
    }

    /**
     * Convert online order to kitchen order (for KDS)
     * Requirement 4.7.3 - seamless integration with KDS
     */
    public Order convertToKitchenOrder(OnlineOrder onlineOrder) {
        log.info("Converting online order {} to kitchen order", onlineOrder.orderNumber());

        // Create equivalent Order record for KDS
        Order kitchenOrder = new Order(
                null,
                Integer.parseInt(onlineOrder.orderNumber().substring(4)), // Order number
                null, // No table for online orders
                null,
                onlineOrder.deliveryType().equals("delivery") ? "delivery" : "pickup",
                onlineOrder.items(),
                onlineOrder.total(),
                "pending", // KDS status
                0.0,
                0.0,
                onlineOrder.paymentMethod(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(30), // Served time
                "confirmed", // Payment status
                onlineOrder.customerName(),
                onlineOrder.customerPhone(),
                onlineOrder.notes(),
                "", // Waiter name
                onlineOrder.orderNumber());

        return kitchenOrder;
    }

    /**
     * Get pending online orders (for restaurant staff to confirm)
     */
    public List<OnlineOrder> getPendingOrders() {
        return onlineOrderRepository.findByStatus("pending_payment");
    }

    /**
     * Get confirmed orders ready for kitchen
     */
    public List<OnlineOrder> getConfirmedOrders() {
        return onlineOrderRepository.findByStatus("confirmed");
    }
}
