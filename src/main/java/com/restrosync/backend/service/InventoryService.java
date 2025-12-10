package com.restrosync.backend.service;

import com.restrosync.backend.model.InventoryItem;
import com.restrosync.backend.dto.InventoryDtos.DeductRequest;
import com.restrosync.backend.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public List<InventoryItem> getAll() {
        return inventoryRepository.findAll();
    }

    public InventoryItem create(InventoryItem item) {
        return inventoryRepository.save(item);
    }

    public InventoryItem update(String id, InventoryItem item) {
        return inventoryRepository.findById(id)
                .map(existing -> {
                    InventoryItem updated = new InventoryItem(
                            id, item.name(), item.unit(), item.currentStock(),
                            item.lowStockAlert(), item.category(), item.costPrice());
                    return inventoryRepository.save(updated);
                })
                .orElse(null);
    }

    public boolean addStock(String id, double amount) {
        return inventoryRepository.findById(id)
                .map(item -> {
                    double newStock = item.currentStock() + amount;
                    InventoryItem updated = item.withStock(newStock);
                    inventoryRepository.save(updated);
                    log.info("Added stock to {}: newStock={}", item.name(), newStock);
                    return true;
                }).orElse(false);
    }

    public void deductStock(DeductRequest request) {
        // Placeholder: keep logic simple for now — user can extend to use menu recipe
        request.items().forEach(cartItem -> {
            // No-op: this endpoint is a stub for future recipe-based deduction
            log.debug("Deduct request item: {} qty {}", cartItem.menuItemId(), cartItem.qty());
        });
    }
}
