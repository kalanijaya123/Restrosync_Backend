package com.restrosync.backend.controller;

import com.restrosync.backend.dto.PaymentRequest;
import com.restrosync.backend.model.Order;
import com.restrosync.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public ResponseEntity<Order> payOrder(@RequestBody PaymentRequest request) {
        log.info("Processing payment for order: {}", request.orderId());
        Order paidOrder = paymentService.processPayment(request);
        return ResponseEntity.ok(paidOrder);
    }
}
