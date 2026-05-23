package com.restrosync.backend.controller;

import com.restrosync.backend.model.Invoice;
import com.restrosync.backend.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Get invoice for an order
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Invoice> getInvoiceByOrderId(@PathVariable String orderId) {
        Optional<Invoice> invoice = invoiceService.getInvoiceByOrderId(orderId);
        return invoice.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Mark invoice as printed
     */
    @PostMapping("/{invoiceId}/print")
    public ResponseEntity<String> markAsPrinted(@PathVariable String invoiceId, @RequestHeader String userId) {
        invoiceService.markAsPrinted(invoiceId, userId);
        return ResponseEntity.ok("Invoice marked as printed");
    }
}
