package com.tuhospedaje.dto.common;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int currentPage,
        long totalItems,
        int totalPages
) {
}
