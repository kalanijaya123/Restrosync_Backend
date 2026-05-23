package com.restrosync.backend.repository;

import com.restrosync.backend.model.KitchenStation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KitchenStationRepository extends MongoRepository<KitchenStation, String> {
    Optional<KitchenStation> findByName(String name);

    List<KitchenStation> findByIsActiveTrueOrderByDisplayOrder();
}
