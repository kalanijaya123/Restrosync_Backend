// src/main/java/com/restrosync/backend/controller/MenuController.java
package com.restrosync.backend.controller;

import com.cloudinary.Cloudinary;
import com.restrosync.backend.model.MenuItem;
import com.restrosync.backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174", "http://localhost:5175",
        "https://your-project.vercel.app" })
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final MenuService menuService;

    @Autowired
    private Cloudinary cloudinary;

    @GetMapping
    public List<MenuItem> getAllMenu() {
        return menuService.getAll();
    }

    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = menuService.uploadImage(file);
            return ResponseEntity.ok(imageUrl);
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            return ResponseEntity.badRequest().body("Failed to upload image");
        }
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

    @PostMapping("/json")
    public ResponseEntity<MenuItem> createMenuItemJson(@RequestBody MenuItem menuItem) {
        try {
            MenuItem saved = menuService.createMenuItemFromJson(menuItem);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Failed to create menu item from JSON", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/json/{id}")
    public ResponseEntity<MenuItem> updateMenuItemJson(@PathVariable String id, @RequestBody MenuItem menuItem) {
        try {
            MenuItem updated = menuService.updateMenuItemJson(id, menuItem);
            return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update menu item from JSON", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable String id) {
        menuService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuItem> updateMenuItem(
            @PathVariable String id,
            @RequestParam(value = "payload", required = false) String payload,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sizes", required = false) String sizesJson,
            @RequestParam(value = "recipe", required = false) String recipeJson,
            @RequestParam(value = "extras", required = false) String extrasJson,
            @RequestParam(value = "media", required = false) MultipartFile media) {
        try {
            MenuItem updated = menuService.updateMenuItem(id, payload, name, category, sizesJson, recipeJson,
                    extrasJson, media);
            return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update menu item", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<MenuItem> toggleAvailability(@PathVariable String id) {
        var updated = menuService.toggleAvailability(id);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }
}