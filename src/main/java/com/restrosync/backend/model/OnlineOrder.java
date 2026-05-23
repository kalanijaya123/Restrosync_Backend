package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "online_orders")
public record OnlineOrder(
        @Id String id,
        String orderNumber,
        String customerId,
        String customerName,
        String customerPhone,
        String customerEmail,
        String deliveryAddress,
        String deliveryType, // "delivery", "pickup"
        List<Order.OrderItem> items,
        Double subtotal,
        Double deliveryFee,
        Double discountAmount,
        Double tax,
        Double total,
        String status, // "pending_payment", "confirmed", "preparing", "ready", "out_for_delivery",
                       // "delivered", "cancelled"
        String paymentStatus, // "pending", "paid", "failed"
        String paymentMethod,
        LocalDateTime orderedAt,
        LocalDateTime expectedDeliveryAt,
        LocalDateTime deliveredAt,
        String notes,
        String trackingToken) {
}
