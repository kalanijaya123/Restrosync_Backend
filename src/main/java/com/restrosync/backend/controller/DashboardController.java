package com.restrosync.backend.controller;

import com.restrosync.backend.model.DashboardMetrics;
import com.restrosync.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get today's metrics
     */
    @GetMapping("/today")
    public ResponseEntity<DashboardMetrics> getTodayMetrics() {
        Optional<DashboardMetrics> metrics = dashboardService.getTodayMetrics();
        return metrics.map(ResponseEntity::ok)
                .orElseGet(() -> {
                    DashboardMetrics generated = dashboardService.generateDailyMetrics(LocalDate.now());
                    return ResponseEntity.ok(generated);
                });
    }

    /**
     * Get metrics for specific date
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<DashboardMetrics> getMetricsForDate(@PathVariable String date) {
        LocalDate parsedDate = LocalDate.parse(date);
        Optional<DashboardMetrics> metrics = dashboardService.getMetricsForDate(parsedDate);
        return metrics.map(ResponseEntity::ok)
                .orElseGet(() -> {
                    DashboardMetrics generated = dashboardService.generateDailyMetrics(parsedDate);
                    return ResponseEntity.ok(generated);
                });
    }

    /**
     * Generate metrics for today
     */
    @PostMapping("/generate")
    public ResponseEntity<DashboardMetrics> generateTodayMetrics() {
        return ResponseEntity.ok(dashboardService.generateDailyMetrics(LocalDate.now()));
    }
}
