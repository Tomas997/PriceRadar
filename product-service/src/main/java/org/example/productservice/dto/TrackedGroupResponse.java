package org.example.productservice.dto;

import org.example.productservice.model.TrackedGroup;

import java.time.LocalDateTime;
import java.util.List;

public record TrackedGroupResponse(
        Long id,
        String userEmail,
        Long lastMinPrice,
        LocalDateTime createdAt,
        List<TrackedItemResponse> items,
        boolean hasStaleItems,
        boolean telegramBlocked
) {
    public static TrackedGroupResponse from(TrackedGroup group, boolean hasStaleItems) {
        List<TrackedItemResponse> items = group.getItems().stream()
                .map(TrackedItemResponse::from)
                .toList();
        return new TrackedGroupResponse(
                group.getId(),
                group.getUserEmail(),
                group.getLastMinPrice(),
                group.getCreatedAt(),
                items,
                hasStaleItems,
                group.isTelegramBlocked()
        );
    }
}
