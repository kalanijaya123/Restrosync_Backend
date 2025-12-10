package com.restrosync.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tables")
public record Table(
                @Id String id,
                String number, // Changed to String (e.g., "T1", "VIP-3")
                Integer chairs, // Total number of seats
                Integer reservedSeats, // Number of seats currently reserved
                String status, // "available", "occupied", "reserved"
                String currentOrderId, // Optional: current active order
                Double x, // Position X (%) on floor plan
                Double y // Position Y (%) on floor plan
) {
        // Constructor for easy creation with defaults
        public Table {
                if (status == null)
                        status = "available";
                if (chairs == null)
                        chairs = 4;
                if (reservedSeats == null)
                        reservedSeats = 0;
                if (x == null)
                        x = 50.0;
                if (y == null)
                        y = 50.0;
        }

        // Helper method to get remaining seats
        public Integer remainingSeats() {
                return chairs - reservedSeats;
        }

        // Helper method to check if fully reserved
        public boolean isFullyReserved() {
                return reservedSeats >= chairs;
        }
}