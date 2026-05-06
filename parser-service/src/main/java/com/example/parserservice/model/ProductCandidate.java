package com.example.parserservice.model;

public record ProductCandidate(
        String marketplace,
        String title,
        long price,
        String url,
        boolean inStock
) {}
