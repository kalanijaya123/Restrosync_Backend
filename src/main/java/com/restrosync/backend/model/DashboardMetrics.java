package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "dashboard_metrics")
public record DashboardMetrics(
        @Id String id,
        LocalDate date,
        Double totalSales,
        Integer totalOrders,
        Integer completedOrders,
        Double averageOrderValue,
        Map<String, Integer> topItems, // MenuItem name -> count
        Map<String, Double> itemsRevenue,
        Integer totalCustomers,
        Integer peakHourOrderCount,
        String peakHour,
        Map<String, Integer> staffOrderCounts,
        Double discountGiven,
        Double taxCollected,
        LocalDateTime generatedAt) {
}
