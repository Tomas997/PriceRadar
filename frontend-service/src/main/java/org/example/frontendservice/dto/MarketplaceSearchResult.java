package org.example.frontendservice.dto;

import java.util.List;

public record MarketplaceSearchResult(
        String marketplace,
        List<SearchResultItem> products
) {}
