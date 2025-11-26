package com.restrosync.backend.controller;

import com.restrosync.backend.model.MenuItem;
import com.restrosync.backend.repository.MenuRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
public class MenuController {

    private final MenuRepository menuRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MenuController(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    @GetMapping
    public List<MenuItem> getMenu() {
        return menuRepository.findAll();
    }

    // NEW: ADD ITEM WITH MULTIPLE SIZES + IMAGE
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addMenuItem(
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam(value = "price", required = false) Double price, // old single price
            @RequestParam(value = "sizes", required = false) String sizesJson, // NEW: JSON array of sizes
            @RequestParam(value = "media", required = false) MultipartFile media) {

        try {
            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "menu");
            Files.createDirectories(uploadDir);

            String mediaUrl = null;
            if (media != null && !media.isEmpty()) {
                String original = media.getOriginalFilename();
                String ext = original != null && original.contains(".")
                        ? original.substring(original.lastIndexOf(".")).toLowerCase()
                        : ".jpg";

                String fileName = UUID.randomUUID() + ext;
                Path target = uploadDir.resolve(fileName);
                media.transferTo(target.toFile());
                mediaUrl = "/media/" + fileName;
            }

            List<MenuItem.Size> sizes = null;
            if (sizesJson != null && !sizesJson.trim().isEmpty()) {
                sizes = objectMapper.readValue(sizesJson, new TypeReference<List<MenuItem.Size>>() {
                });
            }

            MenuItem item = new MenuItem(
                    null,
                    name.trim(),
                    category,
                    price, // can be null for new items
                    sizes, // main data now
                    true,
                    mediaUrl);

            MenuItem saved = menuRepository.save(item);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        menuRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<MenuItem> toggleAvailable(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        return menuRepository.findById(id)
                .map(item -> {
                    MenuItem updated = new MenuItem(
                            item.id(),
                            item.name(),
                            item.category(),
                            item.price(),
                            item.sizes(),
                            body.getOrDefault("available", item.available()),
                            item.mediaUrl());
                    return ResponseEntity.ok(menuRepository.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}