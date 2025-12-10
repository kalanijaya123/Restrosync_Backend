package com.restrosync.backend.service;

import com.restrosync.backend.model.User;
import com.restrosync.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;

    public Map<String, Object> register(User user) {
        if (userRepository.findByEmail(user.email()).isPresent()) {
            return Map.of("error", "Email already used");
        }
        if (userRepository.findByUsername(user.username()).isPresent()) {
            return Map.of("error", "Username already taken");
        }
        User saved = userRepository.save(user);
        return Map.of(
                "message", "Account created successfully",
                "user", Map.of("username", saved.username(), "email", saved.email()));
    }

    public Map<String, Object> login(Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !userOpt.get().password().equals(password)) {
            return Map.of("error", "Invalid username or password");
        }

        var user = userOpt.get();
        return Map.of(
                "message", "Login successful",
                "user", Map.of("username", user.username(), "email", user.email()));
    }
}
