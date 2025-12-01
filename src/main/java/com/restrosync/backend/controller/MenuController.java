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
    private static final String UPLOAD_DIR = "uploads/menu/"; // store under uploads/menu, served at /media/

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
            @RequestParam(value = "payload", required = false) String payload,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sizes", required = false) String sizesJson,
            @RequestParam(value = "recipe", required = false, defaultValue = "[]") String recipeJson,
            @RequestParam(value = "extras", required = false, defaultValue = "[]") String extrasJson,
            @RequestParam(value = "media", required = false) MultipartFile media) {

        try {
            String finalName = name;
            String finalCategory = category;
            String finalSizesJson = sizesJson;
            String finalRecipeJson = recipeJson;
            String finalExtrasJson = extrasJson;

            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            if (payload != null && !payload.isBlank()) {
                // payload is a JSON string produced by the frontend
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(payload);
                if (finalName == null)
                    finalName = root.path("name").asText(null);
                if (finalCategory == null)
                    finalCategory = root.path("category").asText(null);
                if ((finalSizesJson == null || finalSizesJson.isBlank()) && root.has("sizes")) {
                    finalSizesJson = mapper.writeValueAsString(root.get("sizes"));
                }
                if ((finalRecipeJson == null || finalRecipeJson.isBlank()) && root.has("recipe")) {
                    finalRecipeJson = mapper.writeValueAsString(root.get("recipe"));
                }
                if ((finalExtrasJson == null || finalExtrasJson.isBlank()) && root.has("extras")) {
                    finalExtrasJson = mapper.writeValueAsString(root.get("extras"));
                }
            }

            String imageUrl = null;
            if (media != null && !media.isEmpty()) {
                String fileName = UUID.randomUUID() + "_" + media.getOriginalFilename();
                Path path = Paths.get(UPLOAD_DIR);
                Files.createDirectories(path);
                Path target = path.resolve(fileName);
                try (var is = media.getInputStream()) {
                    Files.copy(is, target);
                }
                imageUrl = "http://localhost:8080/media/" + fileName;
            }

            List<MenuItem.Size> sizes = finalSizesJson == null || finalSizesJson.isBlank()
                    ? List.of()
                    : mapper.readValue(finalSizesJson,
                            new com.fasterxml.jackson.core.type.TypeReference<List<MenuItem.Size>>() {
                            });

            List<MenuItem.RecipeItem> recipeList = finalRecipeJson == null || finalRecipeJson.isBlank()
                    ? List.of()
                    : mapper.readValue(finalRecipeJson,
                            new com.fasterxml.jackson.core.type.TypeReference<List<MenuItem.RecipeItem>>() {
                            });

            List<MenuItem.ExtraItem> extrasList = finalExtrasJson == null || finalExtrasJson.isBlank()
                    ? List.of()
                    : mapper.readValue(finalExtrasJson,
                            new com.fasterxml.jackson.core.type.TypeReference<List<MenuItem.ExtraItem>>() {
                            });

            MenuItem item = new MenuItem(
                    null,
                    finalName,
                    finalCategory,
                    null,
                    sizes,
                    true,
                    imageUrl,
                    recipeList,
                    extrasList);

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
                            item.sizes(), !item.available(), item.mediaUrl(), item.recipe(), item.extras());
                    return ResponseEntity.ok(menuRepository.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}