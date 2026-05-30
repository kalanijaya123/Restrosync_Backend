package com.restrosync.backend.service;

import com.cloudinary.Cloudinary;
import com.restrosync.backend.model.MenuItem;
import com.restrosync.backend.repository.MenuRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final MenuRepository menuRepository;
    private final Cloudinary cloudinary;
    private static final Path UPLOAD_DIR = Paths.get("uploads", "menu");
    private static final List<String> DEFAULT_MEAL_PERIODS = List.of("Breakfast", "Lunch", "Dinner");

    private List<String> normalizeMealPeriods(List<String> mealPeriods) {
        if (mealPeriods == null || mealPeriods.isEmpty()) {
            return DEFAULT_MEAL_PERIODS;
        }

        List<String> cleaned = mealPeriods.stream()
                .filter(period -> period != null && !period.isBlank())
                .map(String::trim)
                .filter(period -> period.equalsIgnoreCase("All Day")
                        || DEFAULT_MEAL_PERIODS.stream().anyMatch(p -> p.equalsIgnoreCase(period)))
                .distinct()
                .toList();

        if (cleaned.isEmpty() || cleaned.stream().anyMatch(period -> period.equalsIgnoreCase("All Day"))) {
            return DEFAULT_MEAL_PERIODS;
        }

        return DEFAULT_MEAL_PERIODS.stream()
                .filter(period -> cleaned.stream().anyMatch(selected -> selected.equalsIgnoreCase(period)))
                .toList();
    }

    public MenuItem createMenuItem(String payload, String name, String category, String sizesJson,
            String recipeJson, String extrasJson, String mealPeriodsJson, MultipartFile media) {
        log.info(
                "Creating menu item - payload: {}, name: {}, category: {}, sizesJson: {}, recipeJson: {}, extrasJson: {}, mealPeriodsJson: {}, media: {}",
                payload, name, category, sizesJson, recipeJson, extrasJson, mealPeriodsJson,
                media != null ? media.getOriginalFilename() : "null");

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
            List<String> finalMealPeriods = DEFAULT_MEAL_PERIODS;

            if (payload != null && !payload.isBlank()) {
                var root = mapper.readTree(payload);
                if (finalName == null)
                    finalName = root.path("name").asText(null);
                if (finalCategory == null)
                    finalCategory = root.path("category").asText(null);
                if (root.has("mealPeriods")) {
                    finalMealPeriods = mapper.convertValue(root.get("mealPeriods"), new TypeReference<List<String>>() {
                    });
                }
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

            if (mealPeriodsJson != null && !mealPeriodsJson.isBlank()) {
                finalMealPeriods = mapper.readValue(mealPeriodsJson, new TypeReference<List<String>>() {
                });
            }

            String imageUrl = null;
            if (media != null && !media.isEmpty()) {
                String fileName = UUID.randomUUID() + "_" + media.getOriginalFilename();
                Path target = UPLOAD_DIR.resolve(fileName);
                try (var is = media.getInputStream()) {
                    Files.copy(is, target);
                }
                // store absolute media URL
                imageUrl = "http://localhost:8080/media/" + fileName;
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
                    extrasList, normalizeMealPeriods(finalMealPeriods));
            return menuRepository.save(item);
        } catch (Exception e) {
            log.error("Failed to create menu item", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public MenuItem createMenuItemFromJson(MenuItem menuItem) {
        log.info("Creating menu item from JSON: {}", menuItem);
        log.info("MediaUrl received: {}", menuItem.mediaUrl());
        try {
            MenuItem item = new MenuItem(
                    null,
                    menuItem.name(),
                    menuItem.category(),
                    menuItem.price(),
                    menuItem.sizes() != null ? menuItem.sizes() : List.of(),
                    menuItem.available() != null ? menuItem.available() : true,
                    menuItem.mediaUrl(),
                    menuItem.recipe() != null ? menuItem.recipe() : List.of(),
                    menuItem.extras() != null ? menuItem.extras() : List.of(),
                    normalizeMealPeriods(menuItem.mealPeriods()));
            log.info("MenuItem before save - mediaUrl: {}", item.mediaUrl());
            MenuItem saved = menuRepository.save(item);
            log.info("MenuItem after save - ID: {}, mediaUrl: {}", saved.id(), saved.mediaUrl());
            return saved;
        } catch (Exception e) {
            log.error("Failed to create menu item from JSON", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public MenuItem updateMenuItemJson(String id, MenuItem menuItem) {
        log.info("Updating menu item {} from JSON: {}", id, menuItem);
        return menuRepository.findById(id).map(existingItem -> {
            MenuItem updated = new MenuItem(
                    id,
                    menuItem.name() != null ? menuItem.name() : existingItem.name(),
                    menuItem.category() != null ? menuItem.category() : existingItem.category(),
                    menuItem.price() != null ? menuItem.price() : existingItem.price(),
                    menuItem.sizes() != null ? menuItem.sizes() : existingItem.sizes(),
                    menuItem.available() != null ? menuItem.available() : existingItem.available(),
                    menuItem.mediaUrl() != null ? menuItem.mediaUrl() : existingItem.mediaUrl(),
                    menuItem.recipe() != null ? menuItem.recipe() : existingItem.recipe(),
                    menuItem.extras() != null ? menuItem.extras() : existingItem.extras(),
                    normalizeMealPeriods(
                            menuItem.mealPeriods() != null ? menuItem.mealPeriods() : existingItem.mealPeriods()));
            log.info("Updated MenuItem - mediaUrl: {}", updated.mediaUrl());
            return menuRepository.save(updated);
        }).orElse(null);
    }

    public List<MenuItem> getAll() {
        return menuRepository.findAll();
    }

    public String uploadImage(MultipartFile file) throws IOException {
        // Upload to Cloudinary with a folder structure
        Map uploadResult = cloudinary.uploader()
                .upload(file.getBytes(), Map.of("folder", "restrosync/menu"));
        return (String) uploadResult.get("secure_url");
    }

    public void deleteById(String id) {
        menuRepository.deleteById(id);
    }

    public MenuItem toggleAvailability(String id) {
        return menuRepository.findById(id).map(item -> {
            MenuItem updated = new MenuItem(
                    item.id(), item.name(), item.category(), item.price(),
                    item.sizes(), !item.available(), item.mediaUrl(), item.recipe(), item.extras(),
                    item.mealPeriods());
            return menuRepository.save(updated);
        }).orElse(null);
    }

    public MenuItem updateMenuItem(String id, String payload, String name, String category, String sizesJson,
            String recipeJson, String extrasJson, String mealPeriodsJson, MultipartFile media) {
        return menuRepository.findById(id).map(existingItem -> {
            try {
                ObjectMapper mapper = new ObjectMapper();

                String finalName = name != null ? name : existingItem.name();
                String finalCategory = category != null ? category : existingItem.category();
                String finalSizesJson = sizesJson;
                String finalRecipeJson = recipeJson;
                String finalExtrasJson = extrasJson;
                List<String> finalMealPeriods = existingItem.mealPeriods();

                if (payload != null && !payload.isBlank()) {
                    var root = mapper.readTree(payload);
                    if (name == null && root.has("name"))
                        finalName = root.path("name").asText(finalName);
                    if (category == null && root.has("category"))
                        finalCategory = root.path("category").asText(finalCategory);
                    if (root.has("mealPeriods")) {
                        finalMealPeriods = mapper.convertValue(root.get("mealPeriods"),
                                new TypeReference<List<String>>() {
                                });
                    }
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

                if (mealPeriodsJson != null && !mealPeriodsJson.isBlank()) {
                    finalMealPeriods = mapper.readValue(mealPeriodsJson, new TypeReference<List<String>>() {
                    });
                }

                String imageUrl = existingItem.mediaUrl();
                if (media != null && !media.isEmpty()) {
                    Files.createDirectories(UPLOAD_DIR);
                    String fileName = UUID.randomUUID() + "_" + media.getOriginalFilename();
                    Path target = UPLOAD_DIR.resolve(fileName);
                    try (var is = media.getInputStream()) {
                        Files.copy(is, target);
                    }
                    imageUrl = "http://localhost:8080/media/" + fileName;
                }

                List<MenuItem.Size> sizes = finalSizesJson == null || finalSizesJson.isBlank()
                        ? existingItem.sizes()
                        : mapper.readValue(finalSizesJson, new TypeReference<List<MenuItem.Size>>() {
                        });

                List<MenuItem.RecipeItem> recipeList = finalRecipeJson == null || finalRecipeJson.isBlank()
                        ? existingItem.recipe()
                        : mapper.readValue(finalRecipeJson, new TypeReference<List<MenuItem.RecipeItem>>() {
                        });

                List<MenuItem.ExtraItem> extrasList = finalExtrasJson == null || finalExtrasJson.isBlank()
                        ? existingItem.extras()
                        : mapper.readValue(finalExtrasJson, new TypeReference<List<MenuItem.ExtraItem>>() {
                        });

                MenuItem updated = new MenuItem(id, finalName, finalCategory, existingItem.price(), sizes,
                        existingItem.available(), imageUrl, recipeList, extrasList,
                        normalizeMealPeriods(finalMealPeriods));
                return menuRepository.save(updated);
            } catch (Exception e) {
                log.error("Failed to update menu item", e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }).orElse(null);
    }
}
