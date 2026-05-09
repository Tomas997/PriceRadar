package org.example.productservice.dto;

import org.example.productservice.model.TrackedItem;

public record TrackedItemResponse(
        Long id,
        String marketplace,
        String title,
        String url,
        Long currentPrice
) {
    public static TrackedItemResponse from(TrackedItem item) {
        return new TrackedItemResponse(
                item.getId(),
                item.getMarketplace(),
                item.getTitle(),
                item.getUrl(),
                item.getCurrentPrice()
        );
    }
}
