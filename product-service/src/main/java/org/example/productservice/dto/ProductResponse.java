package org.example.productservice.dto;

import org.example.productservice.model.Product;

public record ProductResponse(
        Long id,
        String title,
        String marketplace,
        String url,
        Boolean inStock
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getMarketplace(),
                product.getUrl(),
                product.getInStock()
        );
    }
}