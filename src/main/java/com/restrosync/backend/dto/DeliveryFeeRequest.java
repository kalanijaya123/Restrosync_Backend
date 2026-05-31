package com.restrosync.backend.dto;

public record DeliveryFeeRequest(
        Double customerLatitude,
        Double customerLongitude) {
}
