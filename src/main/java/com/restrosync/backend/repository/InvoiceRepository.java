package com.restrosync.backend.repository;

import com.restrosync.backend.model.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByOrderId(String orderId);

    List<Invoice> findByPaidAtBetween(LocalDateTime from, LocalDateTime to);

    List<Invoice> findByPaymentStatus(String paymentStatus);
}
