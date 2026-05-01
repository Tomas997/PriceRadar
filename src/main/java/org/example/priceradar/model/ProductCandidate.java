package org.example.priceradar.model;

public record ProductCandidate(
        String marketplace,
        String title,
        long price,
        String url,
        boolean inStock
) {}