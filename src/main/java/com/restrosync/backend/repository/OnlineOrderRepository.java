package com.restrosync.backend.repository;

import com.restrosync.backend.model.OnlineOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OnlineOrderRepository extends MongoRepository<OnlineOrder, String> {
    Optional<OnlineOrder> findByOrderNumber(String orderNumber);

    Optional<OnlineOrder> findByTrackingToken(String trackingToken);

    List<OnlineOrder> findByCustomerId(String customerId);

    List<OnlineOrder> findByStatus(String status);

    List<OnlineOrder> findByOrderedAtBetween(LocalDateTime from, LocalDateTime to);
}
