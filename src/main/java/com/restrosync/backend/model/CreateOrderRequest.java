// src/main/java/com/restrosync/backend/model/CreateOrderRequest.java
package com.restrosync.backend.model;

import java.util.List;

public record CreateOrderRequest(
                List<OrderItem> items,
                Double total,
                String tableNumber, // optional, can be null
                String source // "dine-in" or "takeaway"
) {
        public static record OrderItem(
                        String menuItemId,
                        String name,
                        Double price,
                        Integer qty) {
        }
}