package com.restrosync.backend.controller;

import com.restrosync.backend.dto.UserPermissionsRequest;
import com.restrosync.backend.dto.UserResponseDto;
import com.restrosync.backend.mapper.UserMapper;
import com.restrosync.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174", "http://localhost:5175" })
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/staff")
    public ResponseEntity<?> getStaffUsers(@RequestHeader("userId") String managerId) {
        if (!authService.isManager(managerId)) {
            return ResponseEntity.status(403).body("Manager access required");
        }

        List<UserResponseDto> users = authService.findStaffUsers().stream()
                .map(UserMapper::toResponseDto)
                .toList();

        return ResponseEntity.ok(users);
    }

    @PutMapping("/{targetUserId}/permissions")
    public ResponseEntity<?> updatePermissions(
            @PathVariable String targetUserId,
            @RequestHeader("userId") String managerId,
            @RequestBody UserPermissionsRequest request) {
        if (!authService.isManager(managerId)) {
            return ResponseEntity.status(403).body("Manager access required");
        }

        return authService.updatePermissions(
                targetUserId,
                request.canAccessPos(),
                request.canAccessKds(),
                request.canAccessOnlineOrder(),
                request.canManageDiscounts(),
                request.canManageMenu(),
                request.canManageInventory(),
                request.canAccessKitchenStatus(),
                request.canAccessThirdPartyOrders())
                .map(UserMapper::toResponseDto)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}