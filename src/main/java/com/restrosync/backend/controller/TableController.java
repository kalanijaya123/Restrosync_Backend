package com.restrosync.backend.controller;

import com.restrosync.backend.model.Table;
import com.restrosync.backend.repository.TableRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "http://localhost:5173")
public class TableController {

    private final TableRepository tableRepository;

    public TableController(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    @GetMapping
    public List<Table> getAll() {
        return tableRepository.findAll();
    }

    @PostMapping("/init")
    public ResponseEntity<String> initializeTables() {
        tableRepository.deleteAll();
        List<Table> tables = IntStream.rangeClosed(1, 24)
                .mapToObj(i -> new Table(null, i, "free", null))
                .toList();
        tableRepository.saveAll(tables);
        return ResponseEntity.ok("24 tables initialized");
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Table> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        return tableRepository.findById(id)
                .map(table -> {
                    Table updated = new Table(table.id(), table.number(), body.get("status"), table.currentOrderId());
                    return ResponseEntity.ok(tableRepository.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}