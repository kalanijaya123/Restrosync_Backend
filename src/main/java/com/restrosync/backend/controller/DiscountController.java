package com.restrosync.backend.controller;

import com.restrosync.backend.model.Discount;
import com.restrosync.backend.service.DiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
@Slf4j
public class DiscountController {

    private final DiscountService discountService;

    /**
     * Get all public discounts (for customers and staff)
     */
    @GetMapping("/public")
    public ResponseEntity<List<Discount>> getPublicDiscounts() {
        return ResponseEntity.ok(discountService.getPublicDiscounts());
    }

    /**
     * Get all active discounts
     */
    @GetMapping("/active")
    public ResponseEntity<List<Discount>> getActiveDiscounts() {
        return ResponseEntity.ok(discountService.getActiveDiscounts());
    }

    /**
     * Validate discount code (for POS)
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateDiscount(@RequestParam String code, @RequestParam Double orderAmount) {
        Optional<Discount> discount = discountService.validateDiscount(code, orderAmount);

        if (discount.isPresent()) {
            Discount d = discount.get();
            Double discountAmount = discountService.calculateDiscountAmount(d, orderAmount);
            return ResponseEntity.ok(new DiscountValidationResponse(
                    true,
                    d.code(),
                    d.type(),
                    d.value(),
                    discountAmount,
                    orderAmount - discountAmount));
        }

        return ResponseEntity.ok(new DiscountValidationResponse(false, null, null, null, null, orderAmount));
    }

    /**
     * Create new discount (manager only)
     */
    @PostMapping
    public ResponseEntity<Discount> createDiscount(@RequestBody Discount discount, @RequestHeader String userId) {
        return ResponseEntity.ok(discountService.createDiscount(discount, userId));
    }

    /**
     * Deactivate discount
     */
    @DeleteMapping("/{discountId}")
    public ResponseEntity<String> deactivateDiscount(@PathVariable String discountId, @RequestHeader String userId) {
        discountService.deactivateDiscount(discountId, userId);
        return ResponseEntity.ok("Discount deactivated successfully");
    }

    public static class DiscountValidationResponse {
        public boolean isValid;
        public String code;
        public String type;
        public Double value;
        public Double discountAmount;
        public Double finalAmount;

        public DiscountValidationResponse(boolean isValid, String code, String type, Double value,
                Double discountAmount, Double finalAmount) {
            this.isValid = isValid;
            this.code = code;
            this.type = type;
            this.value = value;
            this.discountAmount = discountAmount;
            this.finalAmount = finalAmount;
        }
    }
}
