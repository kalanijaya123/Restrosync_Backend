package com.restrosync.backend.service;

import com.restrosync.backend.model.OnlineOrder;
import com.restrosync.backend.model.Order;
import com.restrosync.backend.repository.OrderRepository;
import com.restrosync.backend.repository.OnlineOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineOrderService {

    private final OnlineOrderRepository onlineOrderRepository;
    private final OrderRepository orderRepository;
    private final SmsNotificationService smsNotificationService;
    private final EmailNotificationService emailNotificationService;
    private final DeliverySettingsService deliverySettingsService;

    /**
     * Create new online order (Requirement 4.7)
     */
    public OnlineOrder createOnlineOrder(OnlineOrder order) {
        log.info("Creating online order for customer: {}", order.customerName());

        String orderNumber = "ONL-" + System.currentTimeMillis();
        String trackingToken = UUID.randomUUID().toString();

        boolean delivery = "delivery".equalsIgnoreCase(order.deliveryType());
        var deliveryCalculation = delivery
                ? deliverySettingsService.calculateFee(order.deliveryLatitude(), order.deliveryLongitude())
                : new com.restrosync.backend.dto.DeliveryFeeResponse(0.0, 0.0);

        double subtotal = order.subtotal() == null ? 0.0 : order.subtotal();
        double discountAmount = order.discountAmount() == null ? 0.0 : order.discountAmount();
        double tax = order.tax() == null ? 0.0 : order.tax();
        double deliveryFee = delivery
                ? (deliveryCalculation.deliveryFee() == null ? 0.0 : deliveryCalculation.deliveryFee())
                : 0.0;
        double total = Math.max(0.0, subtotal - discountAmount + tax + deliveryFee);

        OnlineOrder newOrder = new OnlineOrder(
                null,
                orderNumber,
                null, // Will be set after customer login
                order.customerName(),
                order.customerPhone(),
                order.customerEmail(),
                order.deliveryAddress(),
                delivery ? order.deliveryLatitude() : null,
                delivery ? order.deliveryLongitude() : null,
                delivery ? deliveryCalculation.distanceKm() : null,
                order.deliveryType(),
                order.items(),
                subtotal,
                deliveryFee,
                discountAmount,
                tax,
                total,
                "pending_payment", // Initial status
                order.paymentStatus() != null ? order.paymentStatus()
                        : "card".equalsIgnoreCase(order.paymentMethod()) ? "paid" : "pending", // Payment status
                order.paymentMethod(),
                order.cardHolderName(),
                order.cardLast4(),
                order.cardExpiry(),
                order.cardTransactionRef(),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(30), // Expected delivery in 30 mins for now
                null,
                order.notes(),
                trackingToken);

        OnlineOrder saved = onlineOrderRepository.save(newOrder);

        if (isPaid(saved)) {
            List<String> itemLines = saved.items() == null ? List.of()
                    : saved.items().stream()
                            .map(item -> item.qty() + "x " + item.menuItemName())
                            .toList();

            smsNotificationService.sendInvoiceMessage(
                    saved.customerPhone(),
                    saved.orderNumber(),
                    saved.customerName(),
                    saved.total() == null ? 0.0 : saved.total(),
                    saved.paymentMethod(),
                    itemLines,
                    saved.notes());

            emailNotificationService.sendInvoiceEmail(
                    saved.customerEmail(),
                    saved.orderNumber(),
                    saved.customerName(),
                    saved.total() == null ? 0.0 : saved.total(),
                    saved.paymentMethod(),
                    itemLines,
                    saved.notes());
        }

        return saved;
    }

    /**
     * Get order by tracking token (for customer tracking)
     */
    public Optional<OnlineOrder> getOrderByTrackingToken(String token) {
        return onlineOrderRepository.findByTrackingToken(token);
    }

    /**
     * Get all online orders for POS/management views
     */
    public List<OnlineOrder> getAllOrders() {
        return onlineOrderRepository.findAll().stream()
                .sorted((a, b) -> b.orderedAt().compareTo(a.orderedAt()))
                .toList();
    }

    /**
     * Update order status
     */
    public void updateOrderStatus(String orderId, String newStatus) {
        onlineOrderRepository.findById(orderId).ifPresent(order -> {
            LocalDateTime deliveredAt = ("delivered".equals(newStatus) || "served".equals(newStatus))
                    ? LocalDateTime.now()
                    : order.deliveredAt();
            OnlineOrder updated = new OnlineOrder(
                    order.id(),
                    order.orderNumber(),
                    order.customerId(),
                    order.customerName(),
                    order.customerPhone(),
                    order.customerEmail(),
                    order.deliveryAddress(),
                    order.deliveryLatitude(),
                    order.deliveryLongitude(),
                    order.deliveryDistanceKm(),
                    order.deliveryType(),
                    order.items(),
                    order.subtotal(),
                    order.deliveryFee(),
                    order.discountAmount(),
                    order.tax(),
                    order.total(),
                    newStatus,
                    order.paymentStatus(),
                    order.paymentMethod(),
                    order.cardHolderName(),
                    order.cardLast4(),
                    order.cardExpiry(),
                    order.cardTransactionRef(),
                    order.orderedAt(),
                    order.expectedDeliveryAt(),
                    deliveredAt,
                    order.notes(),
                    order.trackingToken());
            onlineOrderRepository.save(updated);

            // When the online order is accepted for the kitchen, create a matching
            // kitchen-order record so it appears in the KDS pending queue.
            if ("confirmed".equals(newStatus) || "preparing".equals(newStatus)) {
                syncKitchenOrder(updated);
            }

            log.info("Online order {} status updated to: {}", orderId, newStatus);
        });
    }

    /**
     * Accept an online order and send it to the kitchen queue.
     */
    public void acceptOrderForKitchen(String orderId) {
        findOrder(orderId).ifPresent(order -> {
            OnlineOrder accepted = new OnlineOrder(
                    order.id(),
                    order.orderNumber(),
                    order.customerId(),
                    order.customerName(),
                    order.customerPhone(),
                    order.customerEmail(),
                    order.deliveryAddress(),
                    order.deliveryLatitude(),
                    order.deliveryLongitude(),
                    order.deliveryDistanceKm(),
                    order.deliveryType(),
                    order.items(),
                    order.subtotal(),
                    order.deliveryFee(),
                    order.discountAmount(),
                    order.tax(),
                    order.total(),
                    "preparing",
                    order.paymentStatus(),
                    order.paymentMethod(),
                    order.cardHolderName(),
                    order.cardLast4(),
                    order.cardExpiry(),
                    order.cardTransactionRef(),
                    order.orderedAt(),
                    order.expectedDeliveryAt(),
                    order.deliveredAt(),
                    order.notes(),
                    order.trackingToken());

            onlineOrderRepository.save(accepted);
            syncKitchenOrder(accepted);
            log.info("Online order {} accepted and sent to kitchen", orderId);
        });
    }

    private boolean isPaid(OnlineOrder order) {
        if (order == null) {
            return false;
        }

        if ("paid".equalsIgnoreCase(order.paymentStatus())) {
            return true;
        }

        return "card".equalsIgnoreCase(order.paymentMethod());
    }

    private Optional<OnlineOrder> findOrder(String orderKey) {
        Optional<OnlineOrder> byId = onlineOrderRepository.findById(orderKey);
        if (byId.isPresent()) {
            return byId;
        }

        Optional<OnlineOrder> byOrderNumber = onlineOrderRepository.findAll().stream()
                .filter(order -> orderKey.equals(order.orderNumber()))
                .findFirst();
        if (byOrderNumber.isPresent()) {
            return byOrderNumber;
        }

        return onlineOrderRepository.findAll().stream()
                .filter(order -> orderKey.equals(order.trackingToken()))
                .findFirst();
    }

    private void syncKitchenOrder(OnlineOrder onlineOrder) {
        if (onlineOrder.orderNumber() == null || onlineOrder.orderNumber().isBlank()) {
            return;
        }

        boolean alreadySynced = orderRepository.findAll().stream()
                .anyMatch(existing -> onlineOrder.orderNumber().equals(existing.kotToken()));

        if (alreadySynced) {
            return;
        }

        int orderNo = (int) (orderRepository.count() + 1);
        String source = "delivery".equalsIgnoreCase(onlineOrder.deliveryType()) ? "delivery" : "takeaway";
        String kotToken = onlineOrder.orderNumber();

        Order kitchenOrder = new Order(
                null,
                orderNo,
                null,
                null,
                source,
                onlineOrder.items(),
                onlineOrder.total(),
                "pending",
                onlineOrder.total(),
                0.0,
                onlineOrder.paymentMethod(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                onlineOrder.paymentStatus() == null
                        ? ("card".equalsIgnoreCase(onlineOrder.paymentMethod()) ? "paid" : "pending")
                        : onlineOrder.paymentStatus(),
                onlineOrder.customerName(),
                onlineOrder.customerPhone(),
                onlineOrder.notes(),
                "",
                kotToken);

        orderRepository.save(kitchenOrder);
        log.info("Created kitchen order for online order {} with KOT {}", onlineOrder.orderNumber(), kotToken);
    }

    /**
     * Convert online order to kitchen order (for KDS)
     * Requirement 4.7.3 - seamless integration with KDS
     */
    public Order convertToKitchenOrder(OnlineOrder onlineOrder) {
        log.info("Converting online order {} to kitchen order", onlineOrder.orderNumber());

        // Create equivalent Order record for KDS
        Order kitchenOrder = new Order(
                null,
                Integer.parseInt(onlineOrder.orderNumber().substring(4)), // Order number
                null, // No table for online orders
                null,
                onlineOrder.deliveryType().equals("delivery") ? "delivery" : "pickup",
                onlineOrder.items(),
                onlineOrder.total(),
                "pending", // KDS status
                0.0,
                0.0,
                onlineOrder.paymentMethod(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "card".equalsIgnoreCase(onlineOrder.paymentMethod()) ? "paid" : "pending", // Payment status
                onlineOrder.customerName(),
                onlineOrder.customerPhone(),
                onlineOrder.notes(),
                "", // Waiter name
                onlineOrder.orderNumber());

        return kitchenOrder;
    }

    /**
     * Get pending online orders (for restaurant staff to confirm)
     */
    public List<OnlineOrder> getPendingOrders() {
        return onlineOrderRepository.findByStatus("pending_payment");
    }

    /**
     * Get confirmed orders ready for kitchen
     */
    public List<OnlineOrder> getConfirmedOrders() {
        return onlineOrderRepository.findByStatus("confirmed");
    }
}
