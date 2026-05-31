package com.restrosync.backend.dto;

public record UserPermissionsRequest(
                Boolean canAccessPos,
                Boolean canAccessKds,
                Boolean canAccessOnlineOrder,
                Boolean canManageDiscounts,
                Boolean canManageMenu,
                Boolean canManageInventory,
                Boolean canAccessKitchenStatus,
                Boolean canAccessThirdPartyOrders) {
}