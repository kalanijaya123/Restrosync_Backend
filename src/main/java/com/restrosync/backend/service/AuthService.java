package com.restrosync.backend.service;

import com.restrosync.backend.model.User;
import com.restrosync.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public Map<String, Object> register(User user) {
        if (userRepository.findByEmail(user.email()).isPresent()) {
            return Map.of("error", "Email already used");
        }
        if (userRepository.findByUsername(user.username()).isPresent()) {
            return Map.of("error", "Username already taken");
        }

        // Hash the password before saving
        String hashedPassword = passwordEncoder.encode(user.password());
        User userToSave = new User(null, user.username(), user.email(), hashedPassword, user.role());
        User saved = userRepository.save(userToSave);

        log.info("User registered successfully: {}", saved.username());
        return Map.of(
                "message", "Account created successfully",
                "user", Map.of(
                        "id", saved.id(),
                        "username", saved.username(),
                        "email", saved.email(),
                        "role", saved.role()));
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
                "user", Map.of(
                        "id", user.id(),
                        "username", user.username(),
                        "email", user.email(),
                        "role", user.role()));
    }
}
