package org.example.productservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.productservice.dto.PriceEntryResponse;
import org.example.productservice.dto.ProductResponse;
import org.example.productservice.dto.TrackProductRequest;
import org.example.productservice.service.ProductSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductSearchService productSearchService;

    @GetMapping
    public List<ProductResponse> getAll() {
        return productSearchService.getAllTrackedProducts()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/search")
    public List<ProductResponse> search(@RequestParam String query) {
        return productSearchService.searchByTitle(query)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return ProductResponse.from(productSearchService.getProductById(id));
    }

    @PostMapping("/track")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse track(@Valid @RequestBody TrackProductRequest request) {
        return ProductResponse.from(productSearchService.trackProduct(request));
    }

    @GetMapping("/{id}/history")
    public List<PriceEntryResponse> getPriceHistory(@PathVariable Long id) {
        return productSearchService.getPriceHistory(id)
                .stream()
                .map(PriceEntryResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productSearchService.deleteProduct(id);
    }
}