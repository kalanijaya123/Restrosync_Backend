package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * Enhanced User Model with PIN-based authentication and role management
 * Implements Security Requirements (5.3): PIN authentication and role-based
 * access control
 */
@Document("users")
public record UserEnhanced(
        @Id String id,
        String username,
        String email,
        String password, // Hashed password (for future use)
        String pin, // 4-6 digit PIN for quick staff login
        String role, // "waiter", "cashier", "chef", "manager", "admin"
        String section, // Kitchen station or section assigned to this user
        Boolean isActive,
        String restaurantId, // For multi-location support
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLogin,
        String lastLoginDevice) {
    /**
     * Check if user has permission for an action
     */
    public boolean hasPermission(String action) {
        return switch (role) {
            case "manager", "admin" -> true; // Full access
            case "cashier" -> action.matches("(PROCESS_PAYMENT|APPLY_DISCOUNT|VIEW_ORDERS|CLOSE_TABLE)");
            case "waiter" -> action.matches("(TAKE_ORDER|VIEW_ORDER|APPLY_DISCOUNT|PROCESS_PAYMENT)");
            case "chef" -> action.matches("(VIEW_KDS|MARK_READY|UPDATE_ORDER_STATUS)");
            default -> false;
        };
    }

    /**
     * Can user void items or apply manual discounts?
     */
    public boolean canApplyPrivateDiscount() {
        return "manager".equals(role) || "admin".equals(role);
    }
}
