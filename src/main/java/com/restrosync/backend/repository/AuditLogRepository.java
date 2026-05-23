package com.restrosync.backend.repository;

import com.restrosync.backend.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByEntityId(String entityId);

    List<AuditLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
}
