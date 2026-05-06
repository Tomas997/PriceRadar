package org.example.productservice.dto;

import org.example.productservice.model.PriceEntry;

import java.time.LocalDateTime;

public record PriceEntryResponse(
        Long id,
        Long productId,
        Long price,
        LocalDateTime parsedAt
) {
    public static PriceEntryResponse from(PriceEntry entry) {
        return new PriceEntryResponse(
                entry.getId(),
                entry.getProductId(),
                entry.getPrice(),
                entry.getParsedAt()
        );
    }
}