package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("menu")
public record MenuItem(
        @Id String id,
        String name,
        String category,
        double price,
        boolean available) {
}