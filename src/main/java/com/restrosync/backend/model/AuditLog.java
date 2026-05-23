package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "audit_logs")
public record AuditLog(
        @Id String id,
        String userId,
        String username,
        String action, // "LOGIN", "ORDER_CREATED", "PAYMENT_PROCESSED", "MENU_EDITED", etc.
        String entityType, // "Order", "MenuItem", "User", etc.
        String entityId,
        String details, // JSON string of changes
        String ipAddress,
        String userAgent,
        LocalDateTime timestamp) {
}
