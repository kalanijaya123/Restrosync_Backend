package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "discount_settings")
public record DiscountSettingsConfig(
        @Id String id,
        Map<String, Double> monthlyDiscounts,
        Double highestOrderThreshold,
        Double highestOrderDiscountPercent,
        Double cumulativeSpendThreshold,
        Double cumulativeSpendDiscountPercent,
        Double largeOrderThreshold,
        Double largeOrderDiscountPercent,
        Double maxTotalDiscountPercent,
        LocalDateTime updatedAt) {
}