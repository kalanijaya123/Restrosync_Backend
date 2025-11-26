package com.restrosync.backend.model;

import java.util.List;

public record CreateOrderRequest(
        String tableNumber, // optional: table number like "T1" or "2"
        String source,
        List<OrderItem> items,
        double total) {
}
