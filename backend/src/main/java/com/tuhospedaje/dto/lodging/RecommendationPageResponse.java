package com.tuhospedaje.dto.lodging;

import java.util.List;

public record RecommendationPageResponse(
        List<LodgingDTO> lodgings,
        int currentPage,
        long totalItems,
        int totalPages,
        String revision,
        boolean reset) {
}
