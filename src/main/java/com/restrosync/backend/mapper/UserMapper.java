package com.restrosync.backend.mapper;

import com.restrosync.backend.dto.UserCreateDto;
import com.restrosync.backend.dto.UserResponseDto;
import com.restrosync.backend.model.User;

public class UserMapper {

    public static User toEntity(UserCreateDto dto) {
        return new User(null, dto.username(), dto.email(), dto.password(), dto.role());
    }

    public static UserResponseDto toResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.id())
                .username(user.username())
                .email(user.email())
                .role(user.role())
                .build();
    }
}
