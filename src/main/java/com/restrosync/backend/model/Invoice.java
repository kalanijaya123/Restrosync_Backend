package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "invoices")
public record Invoice(
        @Id String id,
        String orderId,
        String invoiceNumber,
        String tableId,
        String customerName,
        String customerPhone,
        Double subtotal,
        Double discountAmount,
        Double tax,
        Double total,
        String paymentMethod,
        String paymentStatus, // "pending", "paid", "cancelled"
        LocalDateTime issuedAt,
        LocalDateTime paidAt,
        String notes,
        String printedBy,
        Boolean isPrinted) {
}
