// src/main/java/com/restrosync/backend/controller/MenuController.java
package com.restrosync.backend.controller;

import com.restrosync.backend.model.MenuItem;
import com.restrosync.backend.repository.MenuRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "http://localhost:5173")
public class MenuController {

    private final MenuRepository menuRepository;
    private static final String UPLOAD_DIR = "uploads/menu-images/"; // ← MUST BE THIS

    public MenuController(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
        // Create upload folder if not exists
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (Exception e) {
            System.err.println("Could not create upload directory");
        }
    }

    @GetMapping
    public List<MenuItem> getAllMenu() {
        return menuRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<MenuItem> createMenuItem(
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam("sizes") String sizesJson,
            @RequestParam(value = "recipe", required = false, defaultValue = "[]") String recipeJson,
            @RequestParam(value = "media", required = false) MultipartFile media) {

        try {
            String imageUrl = null;
            if (media != null && !media.isEmpty()) {
                String fileName = UUID.randomUUID() + "_" + media.getOriginalFilename();
                Path path = Paths.get(UPLOAD_DIR + fileName);
                Files.write(path, media.getBytes());
                imageUrl = "http://localhost:8080/images/" + fileName; // ← matches /images/** // served via static
            }

            MenuItem item = new MenuItem(
                    null,
                    name,
                    category,
                    null, // legacy price
                    new com.fasterxml.jackson.databind.ObjectMapper().readValue(sizesJson,
                            new com.fasterxml.jackson.core.type.TypeReference<>() {
                            }),
                    true,
                    imageUrl,
                    new com.fasterxml.jackson.databind.ObjectMapper().readValue(recipeJson,
                            new com.fasterxml.jackson.core.type.TypeReference<>() {
                            }));

            MenuItem saved = menuRepository.save(item);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable String id) {
        menuRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<MenuItem> toggleAvailability(@PathVariable String id) {
        return menuRepository.findById(id)
                .map(item -> {
                    MenuItem updated = new MenuItem(
                            item.id(), item.name(), item.category(), item.price(),
                            item.sizes(), !item.available(), item.mediaUrl(), item.recipe());
                    return ResponseEntity.ok(menuRepository.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}