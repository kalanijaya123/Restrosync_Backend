package com.restrosync.backend.service;

import com.restrosync.backend.model.DiscountSettingsConfig;
import com.restrosync.backend.repository.DiscountSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiscountSettingsService {

    private static final String GLOBAL_SETTINGS_ID = "global";
    private static final List<String> MONTH_KEYS = List.of(
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december");

    private final DiscountSettingsRepository repository;

    public DiscountSettingsConfig getSettings() {
        return repository.findById(GLOBAL_SETTINGS_ID)
                .map(this::normalize)
                .orElseGet(this::defaultSettings);
    }

    public DiscountSettingsConfig saveSettings(DiscountSettingsConfig settings) {
        DiscountSettingsConfig normalized = normalize(settings);
        DiscountSettingsConfig saved = new DiscountSettingsConfig(
                GLOBAL_SETTINGS_ID,
                normalized.monthlyDiscounts(),
                normalized.highestOrderThreshold(),
                normalized.highestOrderDiscountPercent(),
                normalized.cumulativeSpendThreshold(),
                normalized.cumulativeSpendDiscountPercent(),
                normalized.largeOrderThreshold(),
                normalized.largeOrderDiscountPercent(),
                normalized.maxTotalDiscountPercent(),
                LocalDateTime.now());
        return repository.save(saved);
    }

    private DiscountSettingsConfig defaultSettings() {
        return new DiscountSettingsConfig(
                GLOBAL_SETTINGS_ID,
                defaultMonthlyDiscounts(),
                10000.0,
                5.0,
                50000.0,
                4.0,
                5000.0,
                3.0,
                20.0,
                LocalDateTime.now());
    }

    private DiscountSettingsConfig normalize(DiscountSettingsConfig settings) {
        DiscountSettingsConfig fallback = defaultSettings();
        if (settings == null) {
            return fallback;
        }

        return new DiscountSettingsConfig(
                GLOBAL_SETTINGS_ID,
                normalizeMonthlyDiscounts(settings.monthlyDiscounts(), fallback.monthlyDiscounts()),
                normalizeAmount(settings.highestOrderThreshold(), fallback.highestOrderThreshold()),
                normalizePercent(settings.highestOrderDiscountPercent(), fallback.highestOrderDiscountPercent()),
                normalizeAmount(settings.cumulativeSpendThreshold(), fallback.cumulativeSpendThreshold()),
                normalizePercent(settings.cumulativeSpendDiscountPercent(), fallback.cumulativeSpendDiscountPercent()),
                normalizeAmount(settings.largeOrderThreshold(), fallback.largeOrderThreshold()),
                normalizePercent(settings.largeOrderDiscountPercent(), fallback.largeOrderDiscountPercent()),
                normalizePercent(settings.maxTotalDiscountPercent(), fallback.maxTotalDiscountPercent()),
                settings.updatedAt() != null ? settings.updatedAt() : LocalDateTime.now());
    }

    private Map<String, Double> defaultMonthlyDiscounts() {
        Map<String, Double> defaults = new LinkedHashMap<>();
        for (String month : MONTH_KEYS) {
            defaults.put(month, 0.0);
        }
        return defaults;
    }

    private Map<String, Double> normalizeMonthlyDiscounts(Map<String, Double> source, Map<String, Double> fallback) {
        Map<String, Double> normalized = new LinkedHashMap<>();
        for (String month : MONTH_KEYS) {
            Double fallbackValue = fallback.getOrDefault(month, 0.0);
            Double rawValue = source != null ? source.getOrDefault(month, fallbackValue) : fallbackValue;
            normalized.put(month, normalizePercent(rawValue, fallbackValue));
        }
        return normalized;
    }

    private Double normalizePercent(Double value, Double fallback) {
        double safe = value != null ? value : fallback;
        if (Double.isNaN(safe) || Double.isInfinite(safe)) {
            return fallback;
        }
        return Math.max(0.0, Math.min(100.0, safe));
    }

    private Double normalizeAmount(Double value, Double fallback) {
        double safe = value != null ? value : fallback;
        if (Double.isNaN(safe) || Double.isInfinite(safe)) {
            return fallback;
        }
        return Math.max(0.0, safe);
    }
}