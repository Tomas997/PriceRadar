package org.example.frontendservice.dto;

import java.util.List;

public record TrackedGroupResponse(
        Long id,
        String userEmail,
        Long lastMinPrice,
        String createdAt,
        List<TrackedItemResponse> items,
        boolean hasStaleItems
) {
    public TrackedItemResponse minPriceItem() {
        if (items == null || items.isEmpty()) return null;
        TrackedItemResponse min = null;
        for (TrackedItemResponse item : items) {
            if (item.currentPrice() == null) continue;
            if (min == null || item.currentPrice() < min.currentPrice()) min = item;
        }
        return min;
    }
}
