package com.restrosync.backend.repository;

import com.restrosync.backend.model.DashboardMetrics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DashboardMetricsRepository extends MongoRepository<DashboardMetrics, String> {
    Optional<DashboardMetrics> findByDate(LocalDate date);
}
