package com.restrosync.backend.dto;

import lombok.Data;

/**
 * DTO for PIN-based authentication (SEC-003)
 */
@Data
public class PinLoginRequest {
    private String pin;
    private String deviceId; // Optional: for tracking which device logged in
}
