package com.restrosync.backend.service;

import com.restrosync.backend.model.Invoice;
import com.restrosync.backend.model.Order;
import com.restrosync.backend.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final AuditService auditService;

    /**
     * Generate invoice from completed order (Payment Processing requirement)
     */
    public Invoice generateInvoice(Order order, String managerId) {
        log.info("Generating invoice for Order #{}", order.orderNo());

        String invoiceNumber = "INV-" + System.currentTimeMillis();

        Invoice invoice = new Invoice(
                null,
                order.id(),
                invoiceNumber,
                order.tableId(),
                order.customerName(),
                order.customerPhone(),
                order.total() - (order.total() - order.total()), // Subtotal (simplified)
                0.0, // Discount will be filled from order
                0.0, // Tax will be calculated
                order.total(),
                order.paymentMethod(),
                "paid",
                LocalDateTime.now(),
                LocalDateTime.now(),
                order.notes(),
                managerId,
                false);

        Invoice saved = invoiceRepository.save(invoice);
        auditService.logAction(managerId, "INVOICE_GENERATED", "Invoice", saved.id(), "For Order: " + order.id());
        return saved;
    }

    /**
     * Get invoice by order ID
     */
    public Optional<Invoice> getInvoiceByOrderId(String orderId) {
        return invoiceRepository.findByOrderId(orderId);
    }

    /**
     * Mark invoice as printed
     */
    public void markAsPrinted(String invoiceId, String userId) {
        invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
            Invoice updated = new Invoice(
                    invoice.id(),
                    invoice.orderId(),
                    invoice.invoiceNumber(),
                    invoice.tableId(),
                    invoice.customerName(),
                    invoice.customerPhone(),
                    invoice.subtotal(),
                    invoice.discountAmount(),
                    invoice.tax(),
                    invoice.total(),
                    invoice.paymentMethod(),
                    invoice.paymentStatus(),
                    invoice.issuedAt(),
                    invoice.paidAt(),
                    invoice.notes(),
                    userId,
                    true);
            invoiceRepository.save(updated);
            auditService.logAction(userId, "INVOICE_PRINTED", "Invoice", invoiceId, "");
        });
    }
}
