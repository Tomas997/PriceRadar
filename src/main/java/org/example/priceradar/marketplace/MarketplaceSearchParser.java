package org.example.priceradar.marketplace;

import org.example.priceradar.model.ProductCandidate;

import java.util.List;

public interface MarketplaceSearchParser {
    String marketplaceName();

    List<ProductCandidate> searchProducts(String query);

    List<ProductCandidate> parseProducts(String json);
}