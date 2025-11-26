package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

public record Order(
    String id,
    Integer orderNo,           // NEW
    String tableId,            // Can be null
    String source,             // "dine-in", "takeaway", "UberEats"
    List<OrderItem> items,
    double total,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}

// Simple OrderItem record declared in the same package/file so the type is available to Order.
record OrderItem(
    String id,
    String name,
    Integer quantity,
    double price
) {
}