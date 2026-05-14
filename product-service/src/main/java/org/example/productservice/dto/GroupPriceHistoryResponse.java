package org.example.productservice.dto;

import java.util.List;

public record GroupPriceHistoryResponse(
        Long groupId,
        boolean hasDemo,
        List<ItemSeries> series
) {
    public record ItemSeries(
            Long itemId,
            String marketplace,
            String title,
            List<PricePoint> entries
    ) {}

    public record PricePoint(
            String date,
            Long price
    ) {}
}