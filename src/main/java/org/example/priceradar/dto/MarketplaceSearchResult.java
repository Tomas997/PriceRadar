package org.example.priceradar.dto;

import org.example.priceradar.model.ProductCandidate;

import java.util.List;

public record MarketplaceSearchResult(
        String marketplace,
        List<ProductCandidate> products
) {}