package com.restrosync.backend.dto;

import lombok.Builder;

@Builder
public record UserCreateDto(
                String username,
                String email,
                String password,
                String role) {
}
