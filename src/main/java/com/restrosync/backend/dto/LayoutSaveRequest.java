package com.restrosync.backend.dto;

import java.util.List;

public record LayoutSaveRequest(List<TablePosition> tables) {
}
