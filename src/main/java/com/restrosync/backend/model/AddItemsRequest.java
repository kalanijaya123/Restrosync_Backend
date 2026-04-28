package com.restrosync.backend.model;

import java.util.List;

/**
 * Request to add items to an existing order
 */
public record AddItemsRequest(
        List<CreateOrderRequest.OrderItemReq> items,
        Double additionalTotal // Additional amount to add to order total
) {
}
