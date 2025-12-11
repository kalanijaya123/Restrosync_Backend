package com.restrosync.backend.dto;

public record PaymentRequest(
        String orderId,
        String paymentMethod, // "cash" or "card"
        Double amountReceived // only for cash
) {
}
