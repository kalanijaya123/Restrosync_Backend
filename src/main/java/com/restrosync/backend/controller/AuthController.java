package com.restrosync.backend.controller;

import com.restrosync.backend.model.User;
import com.restrosync.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        var res = authService.register(user);
        if (res.containsKey("error"))
            return ResponseEntity.badRequest().body(res);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        var res = authService.login(credentials);
        if (res.containsKey("error"))
            return ResponseEntity.badRequest().body(res);
        return ResponseEntity.ok(res);
    }
}