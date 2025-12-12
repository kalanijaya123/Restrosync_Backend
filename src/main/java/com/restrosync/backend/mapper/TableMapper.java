package com.restrosync.backend.mapper;

import com.restrosync.backend.dto.TableCreateDto;
import com.restrosync.backend.dto.TableResponseDto;
import com.restrosync.backend.model.Table;

public class TableMapper {

    public static Table toEntity(TableCreateDto dto) {
        return new Table(
                null,
                dto.number(),
                dto.chairs(),
                dto.reservedSeats() != null ? dto.reservedSeats() : 0,
                dto.status(),
                dto.x(),
                dto.y());
    }

    public static TableResponseDto toResponseDto(Table table) {
        return TableResponseDto.builder()
                .id(table.id())
                .number(table.number())
                .chairs(table.chairs())
                .reservedSeats(table.reservedSeats())
                .remainingSeats(table.remainingSeats())
                .status(table.status())
                .x(table.x())
                .y(table.y())
                .build();
    }
}
