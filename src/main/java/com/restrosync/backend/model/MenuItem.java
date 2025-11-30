// src/main/java/com/restrosync/backend/model/MenuItem.java
package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document("menu")
public record MenuItem(
        @Id String id,
        String name,
        String category,
        Double price, // backward compatibility
        List<Size> sizes,
        Boolean available,
        String mediaUrl,

        // ADD THIS – RECIPE FOR INVENTORY DEDUCTION
        List<RecipeItem> recipe) {
    public record Size(String name, Double price) {
    }

    // NEW: Recipe item – links to inventory
    public record RecipeItem(
            String ingredientId, // MongoDB _id from inventory collection
            String ingredientName, // e.g., "Chicken"
            Double quantity // e.g., 0.5 kg per portion
    ) {
    }
}