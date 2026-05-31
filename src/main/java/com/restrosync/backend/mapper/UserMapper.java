package com.restrosync.backend.mapper;

import com.restrosync.backend.dto.UserCreateDto;
import com.restrosync.backend.dto.UserResponseDto;
import com.restrosync.backend.model.User;

public class UserMapper {

    public static User toEntity(UserCreateDto dto) {
        boolean manager = dto.role() != null && dto.role().equalsIgnoreCase("manager");
        return new User(
                null,
                dto.username(),
                dto.email(),
                dto.password(),
                dto.role(),
                manager,
                manager,
                manager,
                manager,
                manager,
                manager,
                manager,
                manager);
    }

    public static UserResponseDto toResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.id())
                .username(user.username())
                .email(user.email())
                .role(user.role())
                .canAccessPos(Boolean.TRUE.equals(user.canAccessPos()))
                .canAccessKds(Boolean.TRUE.equals(user.canAccessKds()))
                .canAccessOnlineOrder(Boolean.TRUE.equals(user.canAccessOnlineOrder()))
                .canManageDiscounts(Boolean.TRUE.equals(user.canManageDiscounts()))
                .canManageMenu(Boolean.TRUE.equals(user.canManageMenu()))
                .canManageInventory(Boolean.TRUE.equals(user.canManageInventory()))
                .canAccessKitchenStatus(Boolean.TRUE.equals(user.canAccessKitchenStatus()))
                .canAccessThirdPartyOrders(Boolean.TRUE.equals(user.canAccessThirdPartyOrders()))
                .build();
    }
}
