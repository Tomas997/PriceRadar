package com.example.parserservice.service;

import com.example.parserservice.dto.MarketplaceSearchResult;
import com.example.parserservice.marketplace.MarketplaceSearchParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParserService {

    private final List<MarketplaceSearchParser> parsers;

    public List<MarketplaceSearchResult> searchAll(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be blank");
        }
        return parsers.parallelStream()
                .map(parser -> {
                    try {
                        return new MarketplaceSearchResult(
                                parser.marketplaceName(),
                                parser.searchProducts(query)
                        );
                    } catch (Exception e) {
                        log.error("Parser '{}' failed for query '{}': {}",
                                parser.marketplaceName(), query, e.getMessage());
                        return new MarketplaceSearchResult(parser.marketplaceName(), List.of());
                    }
                })
                .toList();
    }
}
