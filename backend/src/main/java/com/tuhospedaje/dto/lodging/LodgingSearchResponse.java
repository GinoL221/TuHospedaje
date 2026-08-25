package com.tuhospedaje.dto.lodging;

import java.util.List;

public record LodgingSearchResponse(
        List<LodgingDTO> lodgings,
        int currentPage,
        long totalItems,
        int totalPages,
        long catalogItems
) {
}
