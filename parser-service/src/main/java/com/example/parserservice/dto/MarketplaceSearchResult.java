package com.example.parserservice.dto;

import com.example.parserservice.model.ProductCandidate;

import java.util.List;

public record MarketplaceSearchResult(
        String marketplace,
        List<ProductCandidate> products
) {}
