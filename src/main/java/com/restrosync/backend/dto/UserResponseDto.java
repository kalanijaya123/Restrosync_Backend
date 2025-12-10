package com.restrosync.backend.dto;

import lombok.Builder;

@Builder
public record UserResponseDto(
        String id,
        String username,
        String email) {
}
