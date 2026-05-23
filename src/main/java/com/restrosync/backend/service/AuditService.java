package com.restrosync.backend.service;

import com.restrosync.backend.model.AuditLog;
import com.restrosync.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log an action for audit trail (SEC-003 requirement)
     */
    public void logAction(String userId, String action, String entityType, String entityId, String details) {
        AuditLog auditLog = new AuditLog(
                null,
                userId,
                "", // Username will be fetched from context
                action,
                entityType,
                entityId,
                details,
                "", // IP address from request context
                "", // User agent from request header
                LocalDateTime.now());

        auditLogRepository.save(auditLog);
        log.info("Audit: User {} performed {} on {} (ID: {})", userId, action, entityType, entityId);
    }

    /**
     * Get audit logs for a specific user
     */
    public java.util.List<AuditLog> getUserAuditLog(String userId) {
        return auditLogRepository.findByUserId(userId);
    }

    /**
     * Get audit logs for a specific entity
     */
    public java.util.List<AuditLog> getEntityAuditLog(String entityId) {
        return auditLogRepository.findByEntityId(entityId);
    }
}
