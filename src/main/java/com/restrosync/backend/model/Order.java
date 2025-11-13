package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document("orders")
public record Order(
        @Id String id,
        String tableId,
        List<OrderItem> items,
        double total,
        String status, // "pending", "preparing", "ready", "served", "paid"
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public record OrderItem(String name, int qty, double price) {
    }
}