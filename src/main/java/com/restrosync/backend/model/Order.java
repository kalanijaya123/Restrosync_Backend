// src/main/java/com/restrosync/backend/model/Order.java
package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "orders")
public record Order(
        @Id String id,
        Integer orderNo,
        String tableId,
        String source,
        List<OrderItem> items,
        Double total,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static record OrderItem(
            String menuItemId,
            String name,
            Double price,
            Integer qty) {
    }
}