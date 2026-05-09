package org.example.frontendservice.dto;

public record TrackedItemResponse(
        Long id,
        String marketplace,
        String title,
        String url,
        Long currentPrice
) {}
