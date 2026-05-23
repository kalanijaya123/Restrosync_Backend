package com.restrosync.backend.service;

import com.restrosync.backend.model.DashboardMetrics;
import com.restrosync.backend.model.Order;
import com.restrosync.backend.repository.DashboardMetricsRepository;
import com.restrosync.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final DashboardMetricsRepository dashboardMetricsRepository;
    private final OrderRepository orderRepository;

    /**
     * Generate daily dashboard metrics (Manager Dashboard requirement 2.2.8)
     */
    public DashboardMetrics generateDailyMetrics(LocalDate date) {
        log.info("Generating dashboard metrics for: {}", date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<Order> ordersOfDay = orderRepository.findAll().stream()
                .filter(o -> o.createdAt() != null &&
                        o.createdAt().isAfter(startOfDay) &&
                        o.createdAt().isBefore(endOfDay) &&
                        ("paid".equals(o.paymentStatus())))
                .toList();

        // Calculate metrics
        Double totalSales = ordersOfDay.stream()
                .mapToDouble(Order::total)
                .sum();

        Integer totalOrders = ordersOfDay.size();
        Integer completedOrders = (int) ordersOfDay.stream()
                .filter(o -> "served".equals(o.status()))
                .count();

        Double averageOrderValue = totalOrders > 0 ? totalSales / totalOrders : 0.0;

        // Top items by count
        Map<String, Integer> topItems = ordersOfDay.stream()
                .flatMap(o -> o.items().stream())
                .collect(Collectors.groupingBy(
                        Order.OrderItem::menuItemName,
                        Collectors.summingInt(Order.OrderItem::qty)))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new));

        // Items revenue
        Map<String, Double> itemsRevenue = ordersOfDay.stream()
                .flatMap(o -> o.items().stream())
                .collect(Collectors.groupingBy(
                        Order.OrderItem::menuItemName,
                        Collectors.summingDouble(item -> item.basePrice() * item.qty())));

        Integer totalCustomers = (int) ordersOfDay.stream()
                .map(Order::customerName)
                .distinct()
                .count();

        DashboardMetrics metrics = new DashboardMetrics(
                null,
                date,
                totalSales,
                totalOrders,
                completedOrders,
                averageOrderValue,
                topItems,
                itemsRevenue,
                totalCustomers,
                0, // Peak hour order count - to be calculated
                "", // Peak hour
                new HashMap<>(), // Staff performance
                0.0, // Discount given
                0.0, // Tax collected
                LocalDateTime.now());

        return dashboardMetricsRepository.save(metrics);
    }

    /**
     * Get today's metrics
     */
    public Optional<DashboardMetrics> getTodayMetrics() {
        return dashboardMetricsRepository.findByDate(LocalDate.now());
    }

    /**
     * Get metrics for specific date
     */
    public Optional<DashboardMetrics> getMetricsForDate(LocalDate date) {
        return dashboardMetricsRepository.findByDate(date);
    }
}
