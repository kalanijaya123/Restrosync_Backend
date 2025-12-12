// src/main/java/com/restrosync/backend/controller/InventoryController.java
package com.restrosync.backend.controller;

import com.restrosync.backend.model.InventoryItem;
import com.restrosync.backend.dto.InventoryDtos.DeductRequest;
import com.restrosync.backend.dto.InventoryDtos.StockUpdate;
import com.restrosync.backend.dto.InventoryDtos.CartItem;
import com.restrosync.backend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    // GET all items
    @GetMapping
    public List<InventoryItem> getAll() {
        return inventoryService.getAll();
    }

    // ADD new ingredient
    @PostMapping
    public ResponseEntity<InventoryItem> create(@RequestBody InventoryItem item) {
        InventoryItem saved = inventoryService.create(item);
        return ResponseEntity.ok(saved);
    }

    // UPDATE ingredient
    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> update(@PathVariable String id, @RequestBody InventoryItem item) {
        InventoryItem updated = inventoryService.update(id, item);
        if (updated == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    // ADD STOCK (Purchase)
    @PostMapping("/{id}/add")
    public ResponseEntity<String> addStock(@PathVariable String id, @RequestBody StockUpdate request) {
        boolean ok = inventoryService.addStock(id, request.amount());
        return ok ? ResponseEntity.ok("Stock added") : ResponseEntity.notFound().build();
    }

    // DEDUCT STOCK (when order is sent)
    @PostMapping("/deduct")
    public ResponseEntity<String> deductStock(@RequestBody DeductRequest request) {
        inventoryService.deductStock(request);
        return ResponseEntity.ok("Stock deducted");
    }
}