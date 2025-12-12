package com.restrosync.backend.dto;

import java.util.List;

public class InventoryDtos {
    public record StockUpdate(double amount) {
    }

    public record CartItem(String menuItemId, int qty) {
    }

    public record DeductRequest(List<CartItem> items) {
    }
}
