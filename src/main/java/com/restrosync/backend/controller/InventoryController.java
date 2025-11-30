// src/main/java/com/restrosync/backend/controller/InventoryController.java
package com.restrosync.backend.controller;

import com.restrosync.backend.model.InventoryItem;
import com.restrosync.backend.repository.InventoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:5173")
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    // GET all items
    @GetMapping
    public List<InventoryItem> getAll() {
        return inventoryRepository.findAll();
    }

    // ADD new ingredient
    @PostMapping
    public ResponseEntity<InventoryItem> create(@RequestBody InventoryItem item) {
        InventoryItem saved = inventoryRepository.save(item);
        return ResponseEntity.ok(saved);
    }

    // UPDATE ingredient
    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> update(@PathVariable String id, @RequestBody InventoryItem item) {
        return inventoryRepository.findById(id)
            .map(existing -> {
                InventoryItem updated = new InventoryItem(
                    id, item.name(), item.unit(), item.currentStock(),
                    item.lowStockAlert(), item.category(), item.costPrice()
                );
                return ResponseEntity.ok(inventoryRepository.save(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // ADD STOCK (Purchase)
    @PostMapping("/{id}/add")
    public ResponseEntity<String> addStock(@PathVariable String id, @RequestBody StockUpdate request) {
        return inventoryRepository.findById(id)
            .map(item -> {
                double newStock = item.currentStock() + request.amount();
                InventoryItem updated = item.withStock(newStock);
                inventoryRepository.save(updated);
                return ResponseEntity.ok("Stock added");
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // DEDUCT STOCK (when order is sent)
    @PostMapping("/deduct")
    public ResponseEntity<String> deductStock(@RequestBody DeductRequest request) {
        request.items().forEach(cartItem -> {
            // You can enhance this to use menu recipe later
            // For now, simple example: deduct 1kg rice per Biryani
        });
        return ResponseEntity.ok("Stock deducted");
    }
}

record StockUpdate(double amount) {}
record DeductRequest(List<CartItem> items) {}
record CartItem(String menuItemId, int qty) {}