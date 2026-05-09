package org.example.frontendservice.dto;

public record SearchResultItem(
        String marketplace,
        String title,
        long price,
        String url,
        boolean inStock
) {}
