package com.restrosync.backend.dto;

import lombok.Builder;

@Builder
public record TableCreateDto(
        String number,
        Integer chairs,
        Integer reservedSeats,
        String status,
        String currentOrderId,
        Double x,
        Double y) {
}
