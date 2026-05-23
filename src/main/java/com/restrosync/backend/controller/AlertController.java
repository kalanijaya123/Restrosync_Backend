package com.restrosync.backend.controller;

import com.restrosync.backend.model.StockAlert;
import com.restrosync.backend.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * Get all active stock alerts
     */
    @GetMapping("/active")
    public ResponseEntity<List<StockAlert>> getActiveAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    /**
     * Get alerts by severity
     */
    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<StockAlert>> getAlertsBySeverity(@PathVariable String severity) {
        return ResponseEntity.ok(alertService.getAlertsBySeverity(severity));
    }

    /**
     * Manually trigger stock check
     */
    @PostMapping("/check-stock")
    public ResponseEntity<String> checkStock() {
        alertService.checkAndCreateStockAlerts();
        return ResponseEntity.ok("Stock check completed");
    }

    /**
     * Mark alert as resolved
     */
    @PostMapping("/{alertId}/resolve")
    public ResponseEntity<String> resolveAlert(@PathVariable String alertId) {
        alertService.resolveAlert(alertId);
        return ResponseEntity.ok("Alert resolved");
    }
}
