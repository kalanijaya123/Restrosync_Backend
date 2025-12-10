// src/main/java/com/restrosync/backend/controller/MenuController.java
package com.restrosync.backend.controller;

import com.restrosync.backend.model.MenuItem;
import com.restrosync.backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public List<MenuItem> getAllMenu() {
        return menuService.getAll();
    }

    @PostMapping
    public ResponseEntity<MenuItem> createMenuItem(
            @RequestParam(value = "payload", required = false) String payload,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sizes", required = false) String sizesJson,
            @RequestParam(value = "recipe", required = false, defaultValue = "[]") String recipeJson,
            @RequestParam(value = "extras", required = false, defaultValue = "[]") String extrasJson,
            @RequestParam(value = "media", required = false) MultipartFile media) {
        try {
            MenuItem saved = menuService.createMenuItem(payload, name, category, sizesJson, recipeJson, extrasJson,
                    media);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Failed to create menu item", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable String id) {
        menuService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<MenuItem> toggleAvailability(@PathVariable String id) {
        var updated = menuService.toggleAvailability(id);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }
}