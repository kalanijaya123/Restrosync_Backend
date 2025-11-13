package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("tables")
public record Table(
        @Id String id,
        int number,
        String status, // "free", "occupied", "dirty"
        String currentOrderId) {
}