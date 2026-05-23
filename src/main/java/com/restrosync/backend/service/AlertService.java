package com.restrosync.backend.service;

import com.restrosync.backend.model.StockAlert;
import com.restrosync.backend.repository.StockAlertRepository;
import com.restrosync.backend.repository.InventoryRepository;
import com.restrosync.backend.model.InventoryItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final StockAlertRepository stockAlertRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * Check inventory and create alerts if needed (PERF-005, Safety requirement)
     */
    public void checkAndCreateStockAlerts() {
        log.info("Checking inventory for low stock items...");

        inventoryRepository.findAll().forEach(item -> {
            if (item.currentStock() < item.lowStockAlert()) {
                String severity = item.currentStock() < (item.lowStockAlert() * 0.5) ? "critical" : "warning";

                // Check if alert already exists and is unresolved
                List<StockAlert> existingAlerts = stockAlertRepository.findByInventoryItemId(item.id());
                boolean alertExists = existingAlerts.stream()
                        .anyMatch(alert -> !alert.isResolved());

                if (!alertExists) {
                    StockAlert alert = new StockAlert(
                            null,
                            item.id(),
                            item.name(),
                            item.currentStock(),
                            item.lowStockAlert(),
                            item.unit(),
                            severity,
                            false,
                            LocalDateTime.now(),
                            null);

                    stockAlertRepository.save(alert);
                    log.warn("Stock alert created for {}: {} {} (threshold: {})",
                            item.name(), item.currentStock(), item.unit(), item.lowStockAlert());
                }
            }
        });
    }

    /**
     * Get all active (unresolved) stock alerts
     */
    public List<StockAlert> getActiveAlerts() {
        return stockAlertRepository.findByIsResolvedFalse();
    }

    /**
     * Get alerts by severity
     */
    public List<StockAlert> getAlertsBySeverity(String severity) {
        return stockAlertRepository.findBySeverity(severity);
    }

    /**
     * Mark alert as resolved
     */
    public void resolveAlert(String alertId) {
        stockAlertRepository.findById(alertId).ifPresent(alert -> {
            StockAlert resolved = new StockAlert(
                    alert.id(),
                    alert.inventoryItemId(),
                    alert.itemName(),
                    alert.currentStock(),
                    alert.lowStockThreshold(),
                    alert.unit(),
                    alert.severity(),
                    true,
                    alert.createdAt(),
                    LocalDateTime.now());
            stockAlertRepository.save(resolved);
            log.info("Stock alert resolved for: {}", alert.itemName());
        });
    }
}
