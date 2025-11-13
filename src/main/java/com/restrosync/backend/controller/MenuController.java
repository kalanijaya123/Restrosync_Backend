package com.restrosync.backend.controller;

import com.restrosync.backend.model.MenuItem;
import com.restrosync.backend.repository.MenuRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "http://localhost:5173")
public class MenuController {

    private final MenuRepository menuRepository;

    public MenuController(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    @GetMapping
    public List<MenuItem> getMenu() {
        return menuRepository.findAll();
    }

    @PostMapping("/init")
    public ResponseEntity<String> initMenu() {
        menuRepository.deleteAll();
        List<MenuItem> items = List.of(
                new MenuItem(null, "Classic Burger", "Mains", 12.99, true),
                new MenuItem(null, "Cheese Pizza", "Mains", 15.99, true),
                new MenuItem(null, "Caesar Salad", "Starters", 8.99, true),
                new MenuItem(null, "Coca Cola", "Drinks", 2.99, true),
                new MenuItem(null, "French Fries", "Sides", 4.99, true));
        menuRepository.saveAll(items);
        return ResponseEntity.ok("Menu initialized with 5 items");
    }
}