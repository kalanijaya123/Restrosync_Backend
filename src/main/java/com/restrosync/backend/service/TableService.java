package com.restrosync.backend.service;

import com.restrosync.backend.model.Table;
import com.restrosync.backend.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableService {

    private final TableRepository tableRepository;

    public List<Table> getAll() {
        return tableRepository.findAll();
    }

    public Table create(Table t) {
        return tableRepository.save(t);
    }

    public Table update(String id, Table t) {
        return tableRepository.findById(id)
                .map(existing -> {
                    Table updated = new Table(
                            existing.id(),
                            t.number() != null ? t.number() : existing.number(),
                            t.chairs() != null ? t.chairs() : existing.chairs(),
                            t.reservedSeats() != null ? t.reservedSeats() : existing.reservedSeats(),
                            existing.status(),
                            existing.currentOrderId(),
                            t.x() != null ? t.x() : existing.x(),
                            t.y() != null ? t.y() : existing.y());
                    return tableRepository.save(updated);
                })
                .orElse(null);
    }

    public Table occupy(String id, String orderId) {
        return tableRepository.findById(id)
                .map(table -> {
                    Table occupied = new Table(
                            table.id(),
                            table.number(),
                            table.chairs(),
                            table.chairs(), // When occupied, all seats are taken
                            "occupied",
                            orderId,
                            table.x(),
                            table.y());
                    return tableRepository.save(occupied);
                }).orElse(null);
    }

    public Table clear(String id) {
        return tableRepository.findById(id)
                .map(table -> {
                    Table cleared = new Table(
                            table.id(),
                            table.number(),
                            table.chairs(),
                            0, // Reset reserved seats
                            "available",
                            null,
                            table.x(),
                            table.y());
                    return tableRepository.save(cleared);
                }).orElse(null);
    }

    public void saveLayout(List<Table> tables) {
        tables.forEach(pos -> tableRepository.findById(pos.id()).ifPresent(table -> {
            Table updated = new Table(
                    table.id(),
                    table.number(),
                    table.chairs(),
                    table.reservedSeats(),
                    table.status(),
                    table.currentOrderId(),
                    pos.x(),
                    pos.y());
            tableRepository.save(updated);
        }));
    }

    public void delete(String id) {
        if (tableRepository.existsById(id))
            tableRepository.deleteById(id);
    }

    public void initSampleTables() {
        tableRepository.deleteAll();
        List<Table> samples = List.of(
                new Table(null, "T1", 4, 0, "available", null, 20.0, 25.0),
                new Table(null, "T2", 6, 0, "available", null, 50.0, 25.0),
                new Table(null, "T3", 4, 0, "available", null, 80.0, 25.0),
                new Table(null, "VIP-1", 8, 0, "available", null, 35.0, 70.0),
                new Table(null, "VIP-2", 10, 0, "available", null, 65.0, 70.0));
        tableRepository.saveAll(samples);
    }

    public Table reserveSeats(String id, Integer seatsToReserve) {
        return tableRepository.findById(id)
                .map(table -> {
                    int newReservedSeats = table.reservedSeats() + seatsToReserve;

                    // Check if enough seats available
                    if (newReservedSeats > table.chairs()) {
                        log.warn("Cannot reserve {} seats on table {}. Only {} seats remaining.",
                                seatsToReserve, table.number(), table.remainingSeats());
                        return null;
                    }

                    // Determine new status based on availability
                    // Green (available): reservedSeats = 0
                    // Orange (reserved): 0 < reservedSeats < chairs
                    // Red (reserved): reservedSeats = chairs
                    String newStatus;
                    if (newReservedSeats == 0) {
                        newStatus = "available"; // Green
                    } else if (newReservedSeats >= table.chairs()) {
                        newStatus = "reserved"; // Red (fully reserved)
                    } else {
                        newStatus = "reserved"; // Orange (partially reserved)
                    }

                    Table reserved = new Table(
                            table.id(),
                            table.number(),
                            table.chairs(),
                            newReservedSeats,
                            newStatus,
                            table.currentOrderId(),
                            table.x(),
                            table.y());

                    log.info("Reserved {} seats on table {}. Remaining: {}",
                            seatsToReserve, table.number(), reserved.remainingSeats());

                    return tableRepository.save(reserved);
                }).orElse(null);
    }

    public Table freeSeats(String id, Integer seatsToFree) {
        return tableRepository.findById(id)
                .map(table -> {
                    int newReservedSeats = Math.max(0, table.reservedSeats() - seatsToFree);

                    // Determine new status based on availability
                    // Green (available): reservedSeats = 0
                    // Orange (reserved): 0 < reservedSeats < chairs
                    // Red (reserved): reservedSeats = chairs
                    String newStatus;
                    if (newReservedSeats == 0) {
                        newStatus = "available"; // Green - all seats free
                    } else if (newReservedSeats >= table.chairs()) {
                        newStatus = "reserved"; // Red - fully reserved
                    } else {
                        newStatus = "reserved"; // Orange - partially reserved
                    }

                    Table freed = new Table(
                            table.id(),
                            table.number(),
                            table.chairs(),
                            newReservedSeats,
                            newStatus,
                            newReservedSeats == 0 ? null : table.currentOrderId(), // Clear order if fully free
                            table.x(),
                            table.y());

                    log.info("Freed {} seats on table {}. Remaining reserved: {}",
                            seatsToFree, table.number(), newReservedSeats);

                    return tableRepository.save(freed);
                }).orElse(null);
    }

}
