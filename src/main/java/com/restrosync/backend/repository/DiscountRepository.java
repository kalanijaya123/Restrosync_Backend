package com.restrosync.backend.repository;

import com.restrosync.backend.model.Discount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends MongoRepository<Discount, String> {
    Optional<Discount> findByCode(String code);

    List<Discount> findByIsActiveTrue();

    @Query("{'isActive': true, 'validFrom': {$lte: ?0}, 'validUntil': {$gte: ?0}}")
    List<Discount> findActiveDiscounts(LocalDateTime now);

    List<Discount> findByScope(String scope);
}
