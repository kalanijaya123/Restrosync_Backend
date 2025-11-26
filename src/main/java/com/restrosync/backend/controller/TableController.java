package com.restrosync.backend.controller;

import com.restrosync.backend.model.Table;
import com.restrosync.backend.repository.TableRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "http://localhost:5173")
public class TableController {

    private final TableRepository tableRepository;

    public TableController(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    // GET all tables
    @GetMapping
    public List<Table> getAllTables() {
        return tableRepository.findAll();
    }

    // ADD new table
    @PostMapping
    public ResponseEntity<Table> createTable(@RequestBody CreateTableRequest request) {
        Table newTable = new Table(
                null,
                request.number(),
                request.chairs(),
                "available",
                null,
                request.x() != null ? request.x() : 50.0,
                request.y() != null ? request.y() : 50.0);
        Table saved = tableRepository.save(newTable);
        return ResponseEntity.ok(saved);
    }

    // UPDATE table details (number, chairs) — for editing
    @PutMapping("/{id}")
    public ResponseEntity<Table> updateTable(
            @PathVariable String id,
            @RequestBody UpdateTableRequest request) {

        return tableRepository.findById(id)
                .map(existing -> {
                    Table updated = new Table(
                            existing.id(),
                            request.number() != null ? request.number() : existing.number(),
                            request.chairs() != null ? request.chairs() : existing.chairs(),
                            existing.status(),
                            existing.currentOrderId(),
                            request.x() != null ? request.x() : existing.x(),
                            request.y() != null ? request.y() : existing.y());
                    return ResponseEntity.ok(tableRepository.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // OCCUPY table (when order is created)
    @PutMapping("/{id}/occupy")
    public ResponseEntity<Table> occupyTable(
            @PathVariable String id,
            @RequestBody OccupyTableRequest request) {

        return tableRepository.findById(id)
                .map(table -> {
                    Table occupied = new Table(
                            table.id(),
                            table.number(),
                            table.chairs(),
                            "occupied",
                            request.orderId(),
                            table.x(),
                            table.y());
                    return ResponseEntity.ok(tableRepository.save(occupied));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // This clears a reserved/occupied table when staff clicks it
    @PutMapping("/{id}/clear")
    public ResponseEntity<Table> clearTable(@PathVariable String id) {
        return tableRepository.findById(id)
                .map(table -> {
                    Table cleared = new Table(
                            table.id(),
                            table.number(),
                            table.chairs(),
                            "available", // ← back to available
                            null,
                            table.x(),
                            table.y());
                    return ResponseEntity.ok(tableRepository.save(cleared));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // SAVE layout positions only (drag & drop)
    @PutMapping("/layout")
    public ResponseEntity<String> saveLayout(@RequestBody LayoutSaveRequest request) {
        try {
            request.tables().forEach(pos -> {
                tableRepository.findById(pos.id()).ifPresent(table -> {
                    Table updated = new Table(
                            table.id(),
                            table.number(),
                            table.chairs(),
                            table.status(),
                            table.currentOrderId(),
                            pos.x(),
                            pos.y());
                    tableRepository.save(updated);
                });
            });
            return ResponseEntity.ok("Layout saved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save layout");
        }
    }

    // DELETE table
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(@PathVariable String id) {
        if (tableRepository.existsById(id)) {
            tableRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // INIT sample tables
    @PostMapping("/init")
    public ResponseEntity<String> initSampleTables() {
        tableRepository.deleteAll();
        List<Table> samples = List.of(
                new Table(null, "T1", 4, "available", null, 20.0, 25.0),
                new Table(null, "T2", 6, "available", null, 50.0, 25.0),
                new Table(null, "T3", 4, "available", null, 80.0, 25.0),
                new Table(null, "VIP-1", 8, "available", null, 35.0, 70.0),
                new Table(null, "VIP-2", 10, "available", null, 65.0, 70.0));
        tableRepository.saveAll(samples);
        return ResponseEntity.ok("Sample tables created");
    }
}

// === REQUEST DTOs ===
record CreateTableRequest(String number, Integer chairs, Double x, Double y) {
}

record UpdateTableRequest(String number, Integer chairs, Double x, Double y) {
}

record OccupyTableRequest(String orderId) {
}

record LayoutSaveRequest(List<TablePosition> tables) {
}

record TablePosition(String id, Double x, Double y) {
}