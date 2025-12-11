package com.restrosync.backend.service;

import com.restrosync.backend.dto.PaymentRequest;
import com.restrosync.backend.model.Order;
import com.restrosync.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order processPayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if ("paid".equals(order.paymentStatus())) {
            throw new RuntimeException("Already paid");
        }

        double amountPaid;
        double changeGiven;

        if ("cash".equals(request.paymentMethod())) {
            amountPaid = request.amountReceived();
            changeGiven = request.amountReceived() - order.total();
            if (changeGiven < 0)
                throw new RuntimeException("Insufficient amount");
        } else {
            // Card / Mobile Pay – assume exact amount
            amountPaid = order.total();
            changeGiven = 0.0;
        }

        // Change status to paid_awaiting_kitchen - requires manual send to kitchen
        String newStatus = "paid_awaiting_kitchen";

        Order paidOrder = new Order(
                order.id(),
                order.orderNo(),
                order.tableId(),
                order.source(),
                order.items(),
                order.total(),
                newStatus, // Waiting for send to kitchen action
                amountPaid,
                changeGiven,
                request.paymentMethod(),
                LocalDateTime.now(),
                order.createdAt(),
                LocalDateTime.now(),
                order.servedAt(),
                "paid",
                order.customerName(),
                order.customerPhone(),
                order.notes(),
                order.waiterName(),
                order.kotToken());

        Order saved = orderRepository.save(paidOrder);

        // Don't deduct inventory yet - wait until sent to kitchen

        log.info("Payment processed for order {}: {} paid, {} change. Awaiting send to kitchen.", order.orderNo(),
                amountPaid, changeGiven);
        return saved;
    }
}
