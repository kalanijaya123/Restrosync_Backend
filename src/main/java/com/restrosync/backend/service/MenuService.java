package com.restrosync.backend.service;

import com.restrosync.backend.model.MenuItem;
import com.restrosync.backend.repository.MenuRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final MenuRepository menuRepository;
    private static final Path UPLOAD_DIR = Paths.get("uploads", "menu");

    public MenuItem createMenuItem(String payload, String name, String category, String sizesJson,
            String recipeJson, String extrasJson, MultipartFile media) {
        try {
            Files.createDirectories(UPLOAD_DIR);
        } catch (Exception e) {
            log.error("Could not create upload dir", e);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();

            String finalName = name;
            String finalCategory = category;
            String finalSizesJson = sizesJson;
            String finalRecipeJson = recipeJson;
            String finalExtrasJson = extrasJson;

            if (payload != null && !payload.isBlank()) {
                var root = mapper.readTree(payload);
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
                Path target = UPLOAD_DIR.resolve(fileName);
                try (var is = media.getInputStream()) {
                    Files.copy(is, target);
                }
                // store relative media URL
                imageUrl = "/media/" + fileName;
            }

            List<MenuItem.Size> sizes = finalSizesJson == null || finalSizesJson.isBlank()
                    ? List.of()
                    : mapper.readValue(finalSizesJson, new TypeReference<List<MenuItem.Size>>() {
                    });

            List<MenuItem.RecipeItem> recipeList = finalRecipeJson == null || finalRecipeJson.isBlank()
                    ? List.of()
                    : mapper.readValue(finalRecipeJson, new TypeReference<List<MenuItem.RecipeItem>>() {
                    });

            List<MenuItem.ExtraItem> extrasList = finalExtrasJson == null || finalExtrasJson.isBlank()
                    ? List.of()
                    : mapper.readValue(finalExtrasJson, new TypeReference<List<MenuItem.ExtraItem>>() {
                    });

            MenuItem item = new MenuItem(null, finalName, finalCategory, null, sizes, true, imageUrl, recipeList,
                    extrasList);
            return menuRepository.save(item);
        } catch (Exception e) {
            log.error("Failed to create menu item", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public List<MenuItem> getAll() {
        return menuRepository.findAll();
    }

    public void deleteById(String id) {
        menuRepository.deleteById(id);
    }

    public MenuItem toggleAvailability(String id) {
        return menuRepository.findById(id).map(item -> {
            MenuItem updated = new MenuItem(
                    item.id(), item.name(), item.category(), item.price(),
                    item.sizes(), !item.available(), item.mediaUrl(), item.recipe(), item.extras());
            return menuRepository.save(updated);
        }).orElse(null);
    }
}
