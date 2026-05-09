package org.example.productservice.dto;

public record TrackedItemRequest(
        String marketplace,
        String title,
        String url,
        Long price
) {}
