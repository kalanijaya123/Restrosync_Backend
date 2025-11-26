package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document("menu")
public record MenuItem(
        @Id String id,
        String name,
        String category,
        Double price, // For old items (backward compatibility)
        List<Size> sizes, // NEW: Multiple sizes with prices
        Boolean available,
        String mediaUrl) {
    // Helper record for size + price
    public record Size(String name, Double price) {
    }
}