package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "discounts")
public record Discount(
        @Id String id,
        String code,
        String type, // "percentage", "fixed_amount"
        Double value, // percentage (0-100) or amount
        String scope, // "public", "private"
        Double minOrderValue,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Integer usageLimit,
        Integer usedCount,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy // Manager ID
) {
}
