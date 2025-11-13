package com.restrosync.backend.controller;

import com.restrosync.backend.model.User;
import com.restrosync.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByEmail(user.email()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already used"));
        }
        if (userRepository.findByUsername(user.username()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already taken"));
        }
        User saved = userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "message", "Account created successfully",
                "user", Map.of("username", saved.username(), "email", saved.email())));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !userOpt.get().password().equals(password)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid username or password"));
        }

        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "user", Map.of(
                        "username", user.username(),
                        "email", user.email()

                )));
    }
}