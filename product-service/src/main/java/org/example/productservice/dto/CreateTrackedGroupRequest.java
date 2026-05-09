package org.example.productservice.dto;

import java.util.List;

public record CreateTrackedGroupRequest(
        String userEmail,
        String telegramChatId,
        List<TrackedItemRequest> items
) {}
