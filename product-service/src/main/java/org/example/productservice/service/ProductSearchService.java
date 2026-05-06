package org.example.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.productservice.dto.TrackProductRequest;
import org.example.productservice.exception.ProductNotFoundException;
import org.example.productservice.model.PriceEntry;
import org.example.productservice.model.Product;
import org.example.productservice.repository.PriceEntryRepository;
import org.example.productservice.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final PriceEntryRepository priceEntryRepository;

    @Transactional
    public Product trackProduct(TrackProductRequest request) {
        Product product = productRepository.save(new Product(
                request.title(),
                request.marketplace(),
                request.url(),
                request.inStock()
        ));
        priceEntryRepository.save(new PriceEntry(product.getId(), request.price()));
        log.info("Tracking product id={} title={}", product.getId(), product.getTitle());
        return product;
    }

    public List<Product> getAllTrackedProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public List<PriceEntry> getPriceHistory(Long productId) {
        getProductById(productId); // validates existence
        return priceEntryRepository.findByProductIdOrderByParsedAtDesc(productId);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        priceEntryRepository.deleteByProductId(id);
        productRepository.deleteById(id);
        log.info("Deleted product id={}", id);
    }

    public List<Product> searchByTitle(String query) {
        return productRepository.findByTitleContainingIgnoreCase(query);
    }
}