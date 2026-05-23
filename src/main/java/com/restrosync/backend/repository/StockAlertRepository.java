package com.restrosync.backend.repository;

import com.restrosync.backend.model.StockAlert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAlertRepository extends MongoRepository<StockAlert, String> {
    List<StockAlert> findByIsResolvedFalse();

    List<StockAlert> findBySeverity(String severity);

    List<StockAlert> findByInventoryItemId(String inventoryItemId);
}
