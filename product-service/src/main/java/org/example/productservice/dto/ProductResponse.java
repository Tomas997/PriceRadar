package org.example.productservice.dto;

import org.example.productservice.model.Product;

public record ProductResponse(
        Long id,
        String title,
        String marketplace,
        String url,
        Boolean inStock,
        String userEmail,
        Long latestPrice
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getMarketplace(),
                product.getUrl(),
                product.getInStock(),
                product.getUserEmail(),
                product.getLatestPrice()
        );
    }
}