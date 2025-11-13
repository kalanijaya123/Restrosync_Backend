package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("inventory")
public record InventoryItem(
        @Id String id,
        String name,
        int stock,
        int threshold) {
}