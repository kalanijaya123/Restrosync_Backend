// src/main/java/com/restrosync/backend/model/MenuItem.java
package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.restrosync.backend.model.MenuItem.ExtraItem;

import java.util.List;
import java.util.Map;

@Document("menu")
public record MenuItem(
    @Id String id,
    String name,
    String category,
    Double price, // legacy
    List<Size> sizes,
    Boolean available,
    String mediaUrl,
    List<RecipeItem> recipe,
    List<ExtraItem> extras  // ← NOW SUPPORTED
) {
    public record Size(String name, Double price) {}

 public record RecipeItem(
     String ingredientId,
     String ingredientName,
     Map<String, Double> quantities
 ) {}

 public record ExtraItem(
     String id,                    // e.g. "extra-cheese"
     String name,                  // "Extra Cheese"
     Double price,                 // ₹50
     Double quantityPerUnit,       // 0.05 kg = 50g
     String ingredientId           // MongoDB _id of cheese in inventory
 ) {}
}