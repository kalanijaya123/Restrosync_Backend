package com.restrosync.backend.controller;

import com.restrosync.backend.dto.CreateTableRequest;
import com.restrosync.backend.dto.UpdateTableRequest;
import com.restrosync.backend.dto.OccupyTableRequest;
import com.restrosync.backend.dto.LayoutSaveRequest;
import com.restrosync.backend.dto.ReserveSeatsRequest;
import com.restrosync.backend.dto.FreeSeatsRequest;
import com.restrosync.backend.model.Table;
import com.restrosync.backend.service.TableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Slf4j
public class TableController {

    private final TableService tableService;

    // GET all tables
    @GetMapping
    public List<Table> getAllTables() {
        return tableService.getAll();
    }

    // GET single table by ID
    @GetMapping("/{id}")
    public ResponseEntity<Table> getTableById(@PathVariable String id) {
        return tableService.getAll().stream()
                .filter(t -> t.id().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ADD new table
    @PostMapping
    public ResponseEntity<Table> createTable(@RequestBody CreateTableRequest request) {
        Table newTable = new Table(
                null,
                request.number(),
                request.chairs(),
                0, // Initial reserved seats
                "available",
                request.x() != null ? request.x() : 50.0,
                request.y() != null ? request.y() : 50.0);
        Table saved = tableService.create(newTable);
        return ResponseEntity.ok(saved);
    }

    // UPDATE table details (number, chairs) — for editing
    @PutMapping("/{id}")
    public ResponseEntity<Table> updateTable(
            @PathVariable String id,
            @RequestBody UpdateTableRequest request) {

        Table t = new Table(
                id,
                request.number(),
                request.chairs(),
                null, // Keep existing reserved seats
                null,
                request.x(),
                request.y());
        Table updated = tableService.update(id, t);
        if (updated == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    // OCCUPY table (when order is created)
    @PutMapping("/{id}/occupy")
    public ResponseEntity<Table> occupyTable(
            @PathVariable String id,
            @RequestBody OccupyTableRequest request) {

        Table occupied = tableService.occupy(id, request.orderId());
        if (occupied == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(occupied);
    }

    // RESERVE specific number of seats
    @PutMapping("/{id}/reserve")
    public ResponseEntity<?> reserveSeats(
            @PathVariable String id,
            @RequestBody ReserveSeatsRequest request) {

        Table reserved = tableService.reserveSeats(id, request.seatsToReserve());
        if (reserved == null)
            return ResponseEntity.badRequest().body("Cannot reserve seats - insufficient capacity");
        return ResponseEntity.ok(reserved);
    }

    // FREE specific number of seats
    @PutMapping("/{id}/free")
    public ResponseEntity<?> freeSeats(
            @PathVariable String id,
            @RequestBody FreeSeatsRequest request) {

        Table freed = tableService.freeSeats(id, request.seatsToFree());
        if (freed == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(freed);
    }

    // This clears a reserved/occupied table when staff clicks it
    @PutMapping("/{id}/clear")
    public ResponseEntity<Table> clearTable(@PathVariable String id) {
        Table cleared = tableService.clear(id);
        if (cleared == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(cleared);
    }

    // SAVE layout positions only (drag & drop)
    @PutMapping("/layout")
    public ResponseEntity<String> saveLayout(@RequestBody LayoutSaveRequest request) {
        try {
            var list = request.tables().stream()
                    .map(pos -> new Table(pos.id(), null, null, null, null, pos.x(), pos.y()))
                    .toList();
            tableService.saveLayout(list);
            return ResponseEntity.ok("Layout saved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save layout");
        }
    }

    // DELETE table
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(@PathVariable String id) {
        tableService.delete(id);
        return ResponseEntity.ok().build();
    }

    // INIT sample tables
    @PostMapping("/init")
    public ResponseEntity<String> initSampleTables() {
        tableService.initSampleTables();
        return ResponseEntity.ok("Sample tables created");
    }
}