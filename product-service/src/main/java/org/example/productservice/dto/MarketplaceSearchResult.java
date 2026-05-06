package org.example.productservice.dto;


import org.example.productservice.model.ProductCandidate;

import java.util.List;

public record MarketplaceSearchResult(
        String marketplace,
        List<ProductCandidate> products
) {
}