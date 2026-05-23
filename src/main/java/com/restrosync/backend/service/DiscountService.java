package com.restrosync.backend.service;

import com.restrosync.backend.model.Discount;
import com.restrosync.backend.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final AuditService auditService;

    /**
     * Create a new discount (manager only)
     */
    public Discount createDiscount(Discount discount, String managerId) {
        log.info("Creating new discount code: {}", discount.code());
        Discount saved = discountRepository.save(discount);
        auditService.logAction(managerId, "DISCOUNT_CREATED", "Discount", saved.id(), discount.toString());
        return saved;
    }

    /**
     * Validate and apply discount to order amount
     * Requirements: SEC-003, 5.5 (Business Rules)
     */
    public Optional<Discount> validateDiscount(String code, Double orderAmount) {
        Optional<Discount> discount = discountRepository.findByCode(code);

        if (discount.isEmpty()) {
            log.warn("Discount code not found: {}", code);
            return Optional.empty();
        }

        Discount d = discount.get();
        LocalDateTime now = LocalDateTime.now();

        // Check if active
        if (!d.isActive() || d.validFrom().isAfter(now) || d.validUntil().isBefore(now)) {
            log.warn("Discount code expired or inactive: {}", code);
            return Optional.empty();
        }

        // Check usage limit
        if (d.usageLimit() != null && d.usedCount() >= d.usageLimit()) {
            log.warn("Discount usage limit reached: {}", code);
            return Optional.empty();
        }

        // Check minimum order value
        if (d.minOrderValue() != null && orderAmount < d.minOrderValue()) {
            log.warn("Order amount {} below minimum {}", orderAmount, d.minOrderValue());
            return Optional.empty();
        }

        return Optional.of(d);
    }

    /**
     * Calculate discount amount based on type
     */
    public Double calculateDiscountAmount(Discount discount, Double orderAmount) {
        if (discount.type().equals("percentage")) {
            return orderAmount * (discount.value() / 100);
        } else {
            return discount.value(); // Fixed amount
        }
    }

    /**
     * Get all active discounts
     */
    public List<Discount> getActiveDiscounts() {
        return discountRepository.findActiveDiscounts(LocalDateTime.now());
    }

    /**
     * Get public discount codes (for customers)
     */
    public List<Discount> getPublicDiscounts() {
        return discountRepository.findByScope("public");
    }

    /**
     * Update discount usage count
     */
    public void incrementUsageCount(String discountId) {
        discountRepository.findById(discountId).ifPresent(d -> {
            Discount updated = new Discount(
                    d.id(), d.code(), d.type(), d.value(), d.scope(), d.minOrderValue(),
                    d.validFrom(), d.validUntil(), d.usageLimit(), d.usedCount() + 1,
                    d.isActive(), d.createdAt(), LocalDateTime.now(), d.createdBy());
            discountRepository.save(updated);
        });
    }

    /**
     * Deactivate discount
     */
    public void deactivateDiscount(String discountId, String managerId) {
        discountRepository.findById(discountId).ifPresent(d -> {
            Discount updated = new Discount(
                    d.id(), d.code(), d.type(), d.value(), d.scope(), d.minOrderValue(),
                    d.validFrom(), d.validUntil(), d.usageLimit(), d.usedCount(),
                    false, d.createdAt(), LocalDateTime.now(), d.createdBy());
            discountRepository.save(updated);
            auditService.logAction(managerId, "DISCOUNT_DEACTIVATED", "Discount", discountId, "");
        });
    }
}
