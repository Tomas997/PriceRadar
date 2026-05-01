package org.example.priceradar.service;

import org.example.priceradar.marketplace.MarketplaceSearchParser;
import org.example.priceradar.dto.MarketplaceSearchResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductSearchService {

    private final List<MarketplaceSearchParser> parsers;

    public ProductSearchService(List<MarketplaceSearchParser> parsers) {
        this.parsers = parsers;
    }

    public List<MarketplaceSearchResult> search(String query) {
        return parsers.stream()
                .map(parser -> {
                    try {
                        return new MarketplaceSearchResult(
                                parser.marketplaceName(),
                                parser.searchProducts(query)
                        );
                    } catch (Exception e) {
                        return new MarketplaceSearchResult(
                                parser.marketplaceName(),
                                List.of()
                        );
                    }
                })
                .toList();
    }
}