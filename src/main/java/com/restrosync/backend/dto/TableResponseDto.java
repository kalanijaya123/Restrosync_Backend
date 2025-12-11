package com.restrosync.backend.dto;

import lombok.Builder;

@Builder
public record TableResponseDto(
                String id,
                String number,
                Integer chairs,
                Integer reservedSeats,
                Integer remainingSeats,
                String status,
                Double x,
                Double y) {
}
