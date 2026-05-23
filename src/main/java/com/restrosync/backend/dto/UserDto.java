package com.restrosync.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Enhanced User DTO with role information
 */
@Data
@AllArgsConstructor
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String role;
    private String section; // "waiter", "cashier", "chef", "manager"
    private boolean isActive;
    private String createdAt;
}
