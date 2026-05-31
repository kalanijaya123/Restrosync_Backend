package com.restrosync.backend.service;

import com.restrosync.backend.dto.DeliveryFeeResponse;
import com.restrosync.backend.model.DeliverySettingsConfig;
import com.restrosync.backend.repository.DeliverySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliverySettingsService {

    private static final String GLOBAL_SETTINGS_ID = "global";

    private final DeliverySettingsRepository repository;

    public DeliverySettingsConfig getSettings() {
        return repository.findById(GLOBAL_SETTINGS_ID)
                .map(this::normalize)
                .orElseGet(this::defaultSettings);
    }

    public DeliverySettingsConfig saveSettings(DeliverySettingsConfig settings) {
        DeliverySettingsConfig normalized = normalize(settings);
        DeliverySettingsConfig toSave = new DeliverySettingsConfig(
                GLOBAL_SETTINGS_ID,
                normalized.restaurantLatitude(),
                normalized.restaurantLongitude(),
                normalized.ranges(),
                LocalDateTime.now());
        return repository.save(toSave);
    }

    public DeliveryFeeResponse calculateFee(Double customerLat, Double customerLng) {
        DeliverySettingsConfig settings = getSettings();

        if (settings.restaurantLatitude() == null || settings.restaurantLongitude() == null
                || customerLat == null || customerLng == null) {
            return new DeliveryFeeResponse(0.0, 0.0);
        }

        double distanceKm = haversineKm(
                settings.restaurantLatitude(),
                settings.restaurantLongitude(),
                customerLat,
                customerLng);

        double fee = resolveFee(distanceKm, settings.ranges());
        return new DeliveryFeeResponse(round(distanceKm), round(fee));
    }

    private DeliverySettingsConfig defaultSettings() {
        List<DeliverySettingsConfig.DistanceFeeRange> ranges = List.of(
                new DeliverySettingsConfig.DistanceFeeRange(3.0, 200.0),
                new DeliverySettingsConfig.DistanceFeeRange(6.0, 350.0),
                new DeliverySettingsConfig.DistanceFeeRange(10.0, 500.0));

        return new DeliverySettingsConfig(
                GLOBAL_SETTINGS_ID,
                6.9271,
                79.8612,
                ranges,
                LocalDateTime.now());
    }

    private DeliverySettingsConfig normalize(DeliverySettingsConfig raw) {
        DeliverySettingsConfig fallback = defaultSettings();
        if (raw == null) {
            return fallback;
        }

        List<DeliverySettingsConfig.DistanceFeeRange> normalizedRanges = normalizeRanges(raw.ranges(),
                fallback.ranges());

        return new DeliverySettingsConfig(
                GLOBAL_SETTINGS_ID,
                normalizeLat(raw.restaurantLatitude(), fallback.restaurantLatitude()),
                normalizeLng(raw.restaurantLongitude(), fallback.restaurantLongitude()),
                normalizedRanges,
                raw.updatedAt() != null ? raw.updatedAt() : LocalDateTime.now());
    }

    private List<DeliverySettingsConfig.DistanceFeeRange> normalizeRanges(
            List<DeliverySettingsConfig.DistanceFeeRange> ranges,
            List<DeliverySettingsConfig.DistanceFeeRange> fallback) {

        List<DeliverySettingsConfig.DistanceFeeRange> source = ranges == null || ranges.isEmpty() ? fallback : ranges;

        List<DeliverySettingsConfig.DistanceFeeRange> normalized = new ArrayList<>();
        for (DeliverySettingsConfig.DistanceFeeRange range : source) {
            if (range == null) {
                continue;
            }

            double maxDistanceKm = safePositive(range.maxDistanceKm(), 0.0);
            double fee = safePositive(range.fee(), 0.0);
            if (maxDistanceKm <= 0.0) {
                continue;
            }
            normalized.add(new DeliverySettingsConfig.DistanceFeeRange(maxDistanceKm, fee));
        }

        if (normalized.isEmpty()) {
            normalized.addAll(fallback);
        }

        normalized.sort(Comparator.comparing(DeliverySettingsConfig.DistanceFeeRange::maxDistanceKm));
        return normalized;
    }

    private double resolveFee(double distanceKm, List<DeliverySettingsConfig.DistanceFeeRange> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            return 0.0;
        }

        for (DeliverySettingsConfig.DistanceFeeRange range : ranges) {
            if (distanceKm <= range.maxDistanceKm()) {
                return range.fee() == null ? 0.0 : range.fee();
            }
        }

        DeliverySettingsConfig.DistanceFeeRange last = ranges.get(ranges.size() - 1);
        return last.fee() == null ? 0.0 : last.fee();
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    private double normalizeLat(Double value, Double fallback) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        return Math.max(-90.0, Math.min(90.0, value));
    }

    private double normalizeLng(Double value, Double fallback) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        return Math.max(-180.0, Math.min(180.0, value));
    }

    private double safePositive(Double value, double fallback) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        return Math.max(0.0, value);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
