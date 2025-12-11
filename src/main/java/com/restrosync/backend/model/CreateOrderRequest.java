// src/main/java/com/restrosync/backend/model/CreateOrderRequest.java
package com.restrosync.backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/**
 * This is the EXACT payload your frontend (OrderEntry.tsx) must send
 * when clicking "SEND TO KITCHEN"
 */
public record CreateOrderRequest(

                // Main order items from cart
                List<OrderItemReq> items,

                // Total bill amount (frontend calculated)
                Double total,

                // Optional: for dine-in (can send either tableId OR tableNumber)
                String tableId,     // MongoDB _id of the table
                String tableNumber, // e.g. "Table 05", "T05", or null for takeaway

                // Required: "dine-in" | "takeaway" | "delivery"
                String source,

                // OPTIONAL BUT HIGHLY RECOMMENDED – ADD THESE FOR REAL RESTAURANTS
                @JsonAlias( {
                                "name", "customer_name" }) String customerName, // "Mr. Perera", "Uber Eats #123"
                @JsonAlias({ "telephone", "phone", "mobile", "customer_phone" }) String customerPhone, // "0771234567"
                String notes, // "No onion", "Extra spicy", "Birthday"
                String waiterName // "Nimal", "Auto"

        ){

        // ONE ORDER ITEM (e.g. Large Chicken Kottu ×2 + Extra Cheese ×3)
        public record OrderItemReq(
                        String menuItemId, // MongoDB _id from menu
                        String sizeName, // "Small", "Regular", "Large"
                        Double price, // Base price of that size
                        Integer qty, // How many portions
                        List<SelectedExtraReq> extras // Can be empty list []
        ) {
        }

        // ONE SELECTED EXTRA (e.g. Extra Cheese ×3)
        public record SelectedExtraReq(
                        String extraId, // "extra-cheese-123" from MenuItem.ExtraItem.id
                        Integer qty // 1, 2, 3...
        ) {
        }
}