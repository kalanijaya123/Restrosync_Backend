package com.restrosync.backend.repository;

import com.restrosync.backend.model.DeliverySettingsConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeliverySettingsRepository extends MongoRepository<DeliverySettingsConfig, String> {
}
