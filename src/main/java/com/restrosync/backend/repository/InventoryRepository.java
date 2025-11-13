package com.restrosync.backend.repository;

import com.restrosync.backend.model.InventoryItem;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryRepository extends MongoRepository<InventoryItem, String> {
}