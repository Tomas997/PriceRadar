package org.example.priceradar.controllers;

import org.example.priceradar.dto.MarketplaceSearchResult;
import org.example.priceradar.model.PriceEntry;
import org.example.priceradar.model.Product;
import org.example.priceradar.model.ProductCandidate;
import org.example.priceradar.service.ProductSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    public ProductSearchController(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @GetMapping("/search")
    public List<MarketplaceSearchResult> search(@RequestParam String query) {
        return productSearchService.search(query);
    }

    @PostMapping("/track")
    public Product track(@RequestBody ProductCandidate candidate) {
        return productSearchService.trackProduct(candidate);
    }

    @GetMapping
    public List<Product> getAllTracked() {
        return productSearchService.getAllTrackedProducts();
    }

    @GetMapping("/{id}/history")
    public List<PriceEntry> getPriceHistory(@PathVariable Long id) {
        return productSearchService.getPriceHistory(id);
    }
}