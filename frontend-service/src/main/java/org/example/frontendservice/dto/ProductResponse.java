package org.example.frontendservice.dto;

public record ProductResponse(
        Long id,
        String title,
        String marketplace,
        String url,
        Boolean inStock,
        String userEmail,
        Long latestPrice
) {}
