package com.restrosync.backend.service;

import com.restrosync.backend.dto.OrderResponseDto;
import com.restrosync.backend.mapper.OrderMapper;
import com.restrosync.backend.model.*;
import com.restrosync.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final TableRepository tableRepository;
    private final MenuRepository menuRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderMapper orderMapper;

    private int nextOrderNo() {
        LocalDate today = LocalDate.now();
        return orderRepository.findAll().stream()
                .filter(o -> o.createdAt() != null && o.createdAt().toLocalDate().equals(today))
                .mapToInt(o -> o.orderNo() != null ? o.orderNo() : 0)
                .max().orElse(0) + 1;
    }

    // Deduct inventory using per-size recipe quantities
    public void deductInventoryForOrder(Order order) {
        log.info("🔴 DEDUCTING INVENTORY for Order #{} with {} items", order.orderNo(), order.items().size());
        order.items().forEach(orderItem -> {
            log.info("  → Processing item: {} (size: {}, qty: {})", orderItem.menuItemName(), orderItem.sizeName(),
                    orderItem.qty());
            menuRepository.findById(orderItem.menuItemId()).ifPresent(menuItem -> {
                if (menuItem.recipe() != null) {
                    log.info("    Recipe found with {} ingredients", menuItem.recipe().size());
                    menuItem.recipe().forEach(recipe -> {
                        double qtyPerUnit = 0.0;
                        var quantitiesMap = recipe.quantities();
                        if (quantitiesMap != null && !quantitiesMap.isEmpty()) {
                            String size = orderItem.sizeName();
                            qtyPerUnit = quantitiesMap.getOrDefault(size,
                                    quantitiesMap.getOrDefault("Regular",
                                            quantitiesMap.values().stream().findFirst().orElse(0.0)));
                        }
                        double totalDeduct = qtyPerUnit * orderItem.qty();
                        log.info("    Deducting {} {} from {} (qty per unit: {})", totalDeduct, recipe.ingredientName(),
                                recipe.ingredientName(), qtyPerUnit);
                        inventoryRepository.findById(recipe.ingredientId()).ifPresent(inv -> {
                            double oldStock = inv.currentStock();
                            double newStock = Math.max(0, inv.currentStock() - totalDeduct);
                            inventoryRepository.save(inv.withStock(newStock));
                            log.info("    ✅ Updated inventory: {} {} → {} {}", inv.name(), oldStock, newStock,
                                    inv.unit());
                            if (newStock <= inv.lowStockAlert()) {
                                log.warn("    ⚠️ LOW STOCK ALERT: {} → {} {}", inv.name(), newStock, inv.unit());
                            }
                        });
                    });
                } else {
                    log.warn("    ⚠️ NO RECIPE found for menu item: {}", menuItem.name());
                }

                if (menuItem.extras() != null && orderItem.extras() != null) {
                    log.info("    Processing {} extras for this item", orderItem.extras().size());
                    orderItem.extras().forEach(selectedExtra -> {
                        menuItem.extras().stream()
                                .filter(extra -> extra.id().equals(selectedExtra.extraId()))
                                .findFirst()
                                .ifPresent(extra -> {
                                    double deductExtra = extra.quantityPerUnit() * selectedExtra.qty()
                                            * orderItem.qty();
                                    log.info("    Deducting {} from extra: {}", deductExtra, selectedExtra.name());
                                    inventoryRepository.findById(extra.ingredientId()).ifPresent(inv -> {
                                        double oldStock = inv.currentStock();
                                        double newStock = Math.max(0, inv.currentStock() - deductExtra);
                                        inventoryRepository.save(inv.withStock(newStock));
                                        log.info("    ✅ Updated inventory (extra): {} {} → {} {}", inv.name(), oldStock,
                                                newStock, inv.unit());
                                        if (newStock <= inv.lowStockAlert()) {
                                            log.warn("    ⚠️ LOW STOCK (EXTRA): {} → {}", inv.name(), newStock);
                                        }
                                    });
                                });
                    });
                }
            });
        });
        log.info("🔴 INVENTORY DEDUCTION COMPLETED for Order #{}", order.orderNo());
    }

    public Map<String, Object> createOrder(CreateOrderRequest request) {
        int orderNo = nextOrderNo();

        // Accept either tableId (MongoDB _id) or tableNumber (display name)
        String tableId = request.tableId(); // Direct MongoDB ID from frontend
        String tableNumber = request.tableNumber();
        log.info("Creating order - Received tableId: {}, tableNumber: {}", request.tableId(), request.tableNumber());

        if (tableId == null && request.tableNumber() != null && !request.tableNumber().trim().isEmpty()) {
            // Fallback: look up by table number
            tableId = tableRepository.findByNumber(request.tableNumber()).map(Table::id).orElse(null);
            log.info("Looked up tableId by number '{}': {}", request.tableNumber(), tableId);
        }

        if ((tableNumber == null || tableNumber.trim().isEmpty()) && tableId != null) {
            tableNumber = tableRepository.findById(tableId).map(Table::number).orElse(null);
        }

        if (tableId != null) {
            log.info("Order will be created with tableId: {}", tableId);
        } else {
            log.warn("No tableId found for order - takeaway/delivery order or table not found");
        }

        List<Order.OrderItem> orderItems = new ArrayList<>();
        for (CreateOrderRequest.OrderItemReq i : request.items()) {
            MenuItem menu = menuRepository.findById(i.menuItemId()).orElse(null);
            String itemName = menu != null ? menu.name() : "Unknown";
            List<Order.SelectedExtra> extras = i.extras() == null ? List.of() : i.extras().stream().map(se -> {
                MenuItem.ExtraItem extra = menu != null
                        ? menu.extras().stream().filter(e -> e.id().equals(se.extraId())).findFirst().orElse(null)
                        : null;
                return new Order.SelectedExtra(se.extraId(), extra != null ? extra.name() : "Unknown",
                        extra != null ? extra.price() : 0.0, se.qty(), extra != null ? extra.quantityPerUnit() : 0.0,
                        extra != null ? extra.ingredientId() : null);
            }).toList();

            orderItems.add(new Order.OrderItem(i.menuItemId(), itemName, i.sizeName(), i.price(), i.qty(), extras, 0.0,
                    0.0, 0.0, 0.0));
        }

        Order order = new Order(
                null, orderNo, tableId, tableNumber, request.source(), orderItems, request.total(),
                "payment_pending", // status - not visible to kitchen until paid
                0.0, // amountPaid
                0.0, // changeGiven
                null, // paymentMethod
                null, // paymentTime
                LocalDateTime.now(), // createdAt
                LocalDateTime.now(), // updatedAt
                null, // servedAt
                "pending", // paymentStatus
                request.customerName(), request.customerPhone(),
                request.notes(), request.waiterName(), null);

        Order saved = orderRepository.save(order);
        // Don't deduct inventory yet - wait until payment is completed

        if (tableId != null) {
            tableRepository.findById(tableId).ifPresent(t -> tableRepository
                    .save(new Table(t.id(), t.number(), t.chairs(), t.chairs(), "occupied", t.x(), t.y())));
        }

        Map<String, Object> res = new HashMap<>();
        res.put("order", saved);
        res.put("orderNo", saved.orderNo());
        return res;
    }

    public List<OrderResponseDto> listAllOrders() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::createdAt).reversed())
                .map(orderMapper::toResponseDto)
                .toList();
    }

    public List<OrderResponseDto> listKdsOrders() {
        return orderRepository.findAll().stream()
                .filter(o -> Set.of("pending", "preparing", "ready").contains(o.status()))
                .sorted(Comparator.comparing(Order::createdAt))
                .map(orderMapper::toResponseDto)
                .toList();
    }

    public List<OrderResponseDto> getRecentOrders() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return orderRepository.findAll().stream()
                .filter(o -> o.createdAt() != null && o.createdAt().isAfter(oneHourAgo))
                .sorted(Comparator.comparing(Order::createdAt).reversed())
                .map(orderMapper::toResponseDto)
                .toList();
    }

    public OrderResponseDto updateStatus(String id, String newStatus) {
        if (!Set.of("payment_pending", "paid_awaiting_kitchen", "pending", "preparing", "ready",
                "served", "cancelled")
                .contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status");
        }
        return orderRepository.findById(id).map(order -> {
            Order updated = new Order(
                    order.id(), order.orderNo(), order.tableId(), order.tableNumber(), order.source(),
                    order.items(), order.total(), newStatus,
                    order.amountPaid(), order.changeGiven(), order.paymentMethod(), order.paymentTime(),
                    order.createdAt(), LocalDateTime.now(),
                    newStatus.equals("served") ? LocalDateTime.now() : order.servedAt(),
                    order.paymentStatus(), order.customerName(), order.customerPhone(),
                    order.notes(), order.waiterName(), order.kotToken());
            Order saved = orderRepository.save(updated);
            return orderMapper.toResponseDto(saved);
        }).orElse(null);
    }

    public OrderResponseDto sendToKitchen(String id) {
        log.info("🍳 SEND TO KITCHEN called for order ID: {}", id);
        return orderRepository.findById(id).map(order -> {
            log.info("  Order found: #{} (paymentStatus: {})", order.orderNo(), order.paymentStatus());
            // Verify order is paid
            if (!"paid".equals(order.paymentStatus())) {
                log.error("  ❌ Order must be paid before sending to kitchen. Current status: {}",
                        order.paymentStatus());
                throw new RuntimeException("Order must be paid before sending to kitchen");
            }

            // Generate KOT token if not exists
            String kotToken = order.kotToken();
            if (kotToken == null || kotToken.isEmpty()) {
                LocalDate today = LocalDate.now();
                int kotCount = (int) orderRepository.findAll().stream()
                        .filter(o -> o.kotToken() != null && o.createdAt() != null
                                && o.createdAt().toLocalDate().equals(today))
                        .count() + 1;
                kotToken = "KOT-" + String.format("%03d", kotCount);
            }

            Order sentToKitchen = new Order(
                    order.id(), order.orderNo(), order.tableId(), order.tableNumber(), order.source(),
                    order.items(), order.total(), "pending",
                    order.amountPaid(), order.changeGiven(), order.paymentMethod(), order.paymentTime(),
                    order.createdAt(), LocalDateTime.now(),
                    order.servedAt(), order.paymentStatus(),
                    order.customerName(), order.customerPhone(),
                    order.notes(), order.waiterName(), kotToken);

            Order saved = orderRepository.save(sentToKitchen);

            // Deduct inventory when sent to kitchen
            deductInventoryForOrder(saved);

            log.info("Order {} sent to kitchen with KOT: {}", order.orderNo(), kotToken);
            return orderMapper.toResponseDto(saved);
        }).orElse(null);
    }

    public OrderResponseDto payOrder(String id) {
        return orderRepository.findById(id).map(order -> {
            Order paid = new Order(
                    order.id(), order.orderNo(), order.tableId(), order.tableNumber(), order.source(),
                    order.items(), order.total(), "paid_awaiting_kitchen",
                    order.total(), 0.0, "cash", LocalDateTime.now(),
                    order.createdAt(), LocalDateTime.now(),
                    order.servedAt(), "paid",
                    order.customerName(), order.customerPhone(),
                    order.notes(), order.waiterName(), order.kotToken());
            Order saved = orderRepository.save(paid);

            // Don't free table yet - wait until order is sent to kitchen
            return orderMapper.toResponseDto(saved);
        }).orElse(null);
    }

    /**
     * Add items to an existing order (for orders in "pending" or "ready" status)
     * Recalculates the total and updates inventory if order is already at kitchen
     */
    public OrderResponseDto addItemsToOrder(String id, AddItemsRequest request) {
        log.info("➕ ADD ITEMS TO ORDER called for order ID: {}", id);
        return orderRepository.findById(id).map(order -> {
            // Verify order is in a valid state for adding items
            if (!Set.of("pending", "preparing", "ready").contains(order.status())) {
                log.error("❌ Cannot add items to order in status: {}", order.status());
                throw new RuntimeException("Can only add items to orders that are pending, preparing, or ready");
            }

            // Create order items for new additions
            List<Order.OrderItem> newOrderItems = new ArrayList<>();
            for (CreateOrderRequest.OrderItemReq i : request.items()) {
                MenuItem menu = menuRepository.findById(i.menuItemId()).orElse(null);
                String itemName = menu != null ? menu.name() : "Unknown";
                List<Order.SelectedExtra> extras = i.extras() == null ? List.of() : i.extras().stream().map(se -> {
                    MenuItem.ExtraItem extra = menu != null
                            ? menu.extras().stream().filter(e -> e.id().equals(se.extraId())).findFirst().orElse(null)
                            : null;
                    return new Order.SelectedExtra(se.extraId(), extra != null ? extra.name() : "Unknown",
                            extra != null ? extra.price() : 0.0, se.qty(),
                            extra != null ? extra.quantityPerUnit() : 0.0,
                            extra != null ? extra.ingredientId() : null);
                }).toList();

                newOrderItems.add(
                        new Order.OrderItem(i.menuItemId(), itemName, i.sizeName(), i.price(), i.qty(), extras, 0.0,
                                0.0, 0.0, 0.0));
            }

            // Merge new items with existing items
            List<Order.OrderItem> mergedItems = new ArrayList<>(order.items());
            mergedItems.addAll(newOrderItems);

            // Calculate new total
            double newTotal = order.total() + request.additionalTotal();

            // Create updated order
            Order updated = new Order(
                    order.id(), order.orderNo(), order.tableId(), order.tableNumber(), order.source(),
                    mergedItems, newTotal, order.status(),
                    order.amountPaid(), order.changeGiven(), order.paymentMethod(), order.paymentTime(),
                    order.createdAt(), LocalDateTime.now(),
                    order.servedAt(), order.paymentStatus(),
                    order.customerName(), order.customerPhone(),
                    order.notes(), order.waiterName(), order.kotToken());

            Order saved = orderRepository.save(updated);

            // If order is already in kitchen (pending/preparing/ready), deduct inventory
            // for new items only
            if (Set.of("pending", "preparing", "ready").contains(order.status())) {
                log.info("Order already in kitchen, deducting inventory for {} new items", newOrderItems.size());
                Order tempOrder = new Order(null, null, null, null, null, newOrderItems, 0.0, null, 0.0, 0.0, null,
                        null,
                        null, null, null, null, null, null, null, null, null);
                deductInventoryForOrder(tempOrder);
            }

            log.info("✅ Items added to order #{}, new total: {}", order.orderNo(), newTotal);
            return orderMapper.toResponseDto(saved);
        }).orElse(null);
    }
}
