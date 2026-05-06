package com.example.parserservice.marketplace;

import com.example.parserservice.model.ProductCandidate;

import java.util.List;

public interface MarketplaceSearchParser {
    String marketplaceName();
    List<ProductCandidate> searchProducts(String query);
    List<ProductCandidate> parseProducts(String json);
}
