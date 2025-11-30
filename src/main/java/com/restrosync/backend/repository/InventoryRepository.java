// src/main/java/com/restrosync/backend/repository/InventoryRepository.java
package com.restrosync.backend.repository;

import com.restrosync.backend.model.InventoryItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface InventoryRepository extends MongoRepository<InventoryItem, String> {
    Optional<InventoryItem> findByNameIgnoreCase(String name);
}