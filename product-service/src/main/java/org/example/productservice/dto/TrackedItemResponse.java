package org.example.productservice.dto;

import org.example.productservice.model.CatalogItem;
import org.example.productservice.model.TrackedItem;

public record TrackedItemResponse(
        Long id,
        String marketplace,
        String title,
        String url,
        Long currentPrice
) {
    public static TrackedItemResponse from(TrackedItem item) {
        CatalogItem ci = item.getCatalogItem();
        return new TrackedItemResponse(
                item.getId(),
                ci.getMarketplace(),
                ci.getTitle(),
                ci.getUrl(),
                ci.getCurrentPrice()
        );
    }
}
