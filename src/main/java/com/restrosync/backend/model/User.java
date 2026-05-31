package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public record User(
                @Id String id,
                String username,
                String email,
                String password,
                String role,
                Boolean canAccessPos,
                Boolean canAccessKds,
                Boolean canAccessOnlineOrder,
                Boolean canManageDiscounts,
                Boolean canManageMenu,
                Boolean canManageInventory,
                Boolean canAccessKitchenStatus,
                Boolean canAccessThirdPartyOrders

) {
}