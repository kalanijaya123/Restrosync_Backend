// src/main/java/com/restrosync/backend/model/Order.java
package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "orders")
public record Order(
    @Id String id,
    Integer orderNo,
    String tableId,
    
    String source,                    // "dine-in", "takeaway", "delivery"
    List<OrderItem> items,
    Double total,
    String status, 
          // "pending", "paid", "partially_paid"
    Double amountPaid,             // e.g., 3000.00 when customer gives 3000
    Double changeGiven,            // for cash
    String paymentMethod,          // "cash", "card", "mobile"
    LocalDateTime paymentTime,                   // "pending", "preparing", "ready", "served", "cancelled"
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    // CRITICAL FIELDS FOR REAL RESTAURANT
    LocalDateTime servedAt,
    String paymentStatus,             // "pending", "paid"
    String customerName,
    String customerPhone,
    String notes,                     // "No onion", "Extra spicy"
    String waiterName,
    String kotToken                   // KOT-001, KOT-002
) {

    public static record OrderItem(
        String menuItemId,
        String menuItemName,
        String sizeName,
        Double basePrice,
        Integer qty,
        List<SelectedExtra> extras,

        // THIS IS WHAT MAKES INVENTORY DEDUCTION WORK
        Double chickenUsed,           // e.g. 0.9 kg for 3× Large Kottu
        Double riceUsed,              // e.g. 1.2 kg
        Double cheeseUsed,            // from extras
        Double totalIngredientCost     // optional: for profit calculation
    ) {}

    public static record SelectedExtra(
        String extraId,
        String name,
        Double price,
        Integer qty,

        // THESE 2 FIELDS ARE 100% NEEDED FOR INVENTORY DEDUCTION
        Double quantityPerUnit,       // e.g. 0.05 kg per Extra Cheese
        String ingredientId           // MongoDB ID of ingredient (cheese, egg, etc.)
    ) {}
}