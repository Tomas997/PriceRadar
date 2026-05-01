package org.example.priceradar.Marketplaces;

import org.example.priceradar.model.ProductCandidate;

import java.util.List;

public interface MarketplaceSearchParser {
    String marketplaceName();
    List<ProductCandidate> searchProducts(String query);
}