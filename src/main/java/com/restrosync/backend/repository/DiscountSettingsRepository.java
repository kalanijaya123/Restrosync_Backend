package com.restrosync.backend.repository;

import com.restrosync.backend.model.DiscountSettingsConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountSettingsRepository extends MongoRepository<DiscountSettingsConfig, String> {
}