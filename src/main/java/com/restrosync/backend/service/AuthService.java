package com.restrosync.backend.service;

import com.restrosync.backend.model.User;
import com.restrosync.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private boolean isManagerRole(String role) {
        return role != null && role.equalsIgnoreCase("manager");
    }

    private boolean defaultAccess(boolean managerValue, Boolean providedValue) {
        return providedValue != null ? providedValue : managerValue;
    }

    private User normalizeUserPermissions(User user) {
        boolean manager = isManagerRole(user.role());
        return new User(
                user.id(),
                user.username(),
                user.email(),
                user.password(),
                user.role(),
                defaultAccess(manager, user.canAccessPos()),
                defaultAccess(manager, user.canAccessKds()),
                defaultAccess(manager, user.canAccessOnlineOrder()),
                defaultAccess(manager, user.canManageDiscounts()),
                defaultAccess(manager, user.canManageMenu()),
                defaultAccess(manager, user.canManageInventory()),
                defaultAccess(manager, user.canAccessKitchenStatus()),
                defaultAccess(manager, user.canAccessThirdPartyOrders()));
    }

    private Map<String, Object> buildUserPayload(User user) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("id", user.id());
        payload.put("username", user.username());
        payload.put("email", user.email());
        payload.put("role", user.role());
        payload.put("canAccessPos", Boolean.TRUE.equals(user.canAccessPos()));
        payload.put("canAccessKds", Boolean.TRUE.equals(user.canAccessKds()));
        payload.put("canAccessOnlineOrder", Boolean.TRUE.equals(user.canAccessOnlineOrder()));
        payload.put("canManageDiscounts", Boolean.TRUE.equals(user.canManageDiscounts()));
        payload.put("canManageMenu", Boolean.TRUE.equals(user.canManageMenu()));
        payload.put("canManageInventory", Boolean.TRUE.equals(user.canManageInventory()));
        payload.put("canAccessKitchenStatus", Boolean.TRUE.equals(user.canAccessKitchenStatus()));
        payload.put("canAccessThirdPartyOrders", Boolean.TRUE.equals(user.canAccessThirdPartyOrders()));
        return payload;
    }

    public Map<String, Object> register(User user) {
        if (userRepository.findByEmail(user.email()).isPresent()) {
            return Map.of("error", "Email already used");
        }
        if (userRepository.findByUsername(user.username()).isPresent()) {
            return Map.of("error", "Username already taken");
        }

        // Hash the password before saving
        String hashedPassword = passwordEncoder.encode(user.password());
        User userToSave = normalizeUserPermissions(
                new User(
                        null,
                        user.username(),
                        user.email(),
                        hashedPassword,
                        user.role(),
                        user.canAccessPos(),
                        user.canAccessKds(),
                        user.canAccessOnlineOrder(),
                        user.canManageDiscounts(),
                        user.canManageMenu(),
                        user.canManageInventory(),
                        user.canAccessKitchenStatus(),
                        user.canAccessThirdPartyOrders()));
        User saved = userRepository.save(userToSave);

        log.info("User registered successfully: {}", saved.username());
        return Map.of(
                "message", "Account created successfully",
                "user", buildUserPayload(saved));
    }

    public Map<String, Object> login(Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Map.of("error", "Invalid username or password");
        }

        var user = userOpt.get();

        // Verify password using BCrypt
        if (!passwordEncoder.matches(password, user.password())) {
            return Map.of("error", "Invalid username or password");
        }

        log.info("User logged in successfully: {}", user.username());
        return Map.of(
                "message", "Login successful",
                "user", buildUserPayload(normalizeUserPermissions(user)));
    }

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    public boolean isManager(String userId) {
        return findById(userId)
                .map(user -> isManagerRole(user.role()))
                .orElse(false);
    }

    public List<User> findStaffUsers() {
        return userRepository.findByRole("Staff");
    }

    public Optional<User> updatePermissions(String userId, Boolean canAccessPos, Boolean canAccessKds,
            Boolean canAccessOnlineOrder, Boolean canManageDiscounts, Boolean canManageMenu,
            Boolean canManageInventory, Boolean canAccessKitchenStatus, Boolean canAccessThirdPartyOrders) {
        return userRepository.findById(userId).map(existing -> {
            User updated = new User(
                    existing.id(),
                    existing.username(),
                    existing.email(),
                    existing.password(),
                    existing.role(),
                    canAccessPos != null ? canAccessPos : Boolean.TRUE.equals(existing.canAccessPos()),
                    canAccessKds != null ? canAccessKds : Boolean.TRUE.equals(existing.canAccessKds()),
                    canAccessOnlineOrder != null ? canAccessOnlineOrder
                            : Boolean.TRUE.equals(existing.canAccessOnlineOrder()),
                    canManageDiscounts != null ? canManageDiscounts
                            : Boolean.TRUE.equals(existing.canManageDiscounts()),
                    canManageMenu != null ? canManageMenu : Boolean.TRUE.equals(existing.canManageMenu()),
                    canManageInventory != null ? canManageInventory
                            : Boolean.TRUE.equals(existing.canManageInventory()),
                    canAccessKitchenStatus != null ? canAccessKitchenStatus
                            : Boolean.TRUE.equals(existing.canAccessKitchenStatus()),
                    canAccessThirdPartyOrders != null ? canAccessThirdPartyOrders
                            : Boolean.TRUE.equals(existing.canAccessThirdPartyOrders()));
            return userRepository.save(updated);
        });
    }
}
