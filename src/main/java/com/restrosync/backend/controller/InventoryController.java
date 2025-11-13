package com.restrosync.backend.controller;

import com.restrosync.backend.model.InventoryItem;
import com.restrosync.backend.repository.InventoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:5173")
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping
    public List<InventoryItem> getAll() {
        return inventoryRepository.findAll();
    }

    @PostMapping("/init")
    public ResponseEntity<String> initInventory() {
        inventoryRepository.deleteAll();
        List<InventoryItem> items = List.of(
            new InventoryItem(null, "Burger Patty", 50, 10),
            new InventoryItem(null, "Cheese Slice", 40, 8),
            new InventoryItem(null, "Pizza Dough", 30, 5),
            new InventoryItem(null, "Lettuce", 20, 5),
            new InventoryItem(null, "Coke Can", 100, 20)
        );
        inventoryRepository.saveAll(items);
        return ResponseEntity.ok("Inventory initialized");
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> updateStock(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        return inventoryRepository.findById(id)
                .map(item -> {
                    InventoryItem updated = new InventoryItem(item.id(), item.name(), body.get("stock"), item.threshold());
                    return ResponseEntity.ok(inventoryRepository.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}