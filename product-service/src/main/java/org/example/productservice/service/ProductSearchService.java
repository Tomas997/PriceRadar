package org.example.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.productservice.dto.TrackProductRequest;
import org.example.productservice.exception.ProductNotFoundException;
import org.example.productservice.model.PriceEntry;
import org.example.productservice.model.Product;
import org.example.productservice.repository.PriceEntryRepository;
import org.example.productservice.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final PriceEntryRepository priceEntryRepository;

    @Transactional
    public Product trackProduct(TrackProductRequest request, String userEmail) {
        String effectiveEmail = (userEmail != null && !userEmail.isBlank()) ? userEmail : request.userEmail();
        Product product = productRepository.save(new Product(
                request.title(),
                request.marketplace(),
                request.url(),
                request.inStock(),
                effectiveEmail,
                request.price()
        ));
        priceEntryRepository.save(new PriceEntry(product.getId(), request.price()));
        log.info("Tracking product id={} title={} user={}", product.getId(), product.getTitle(), product.getUserEmail());
        return product;
    }

    public List<Product> getAllTrackedProducts() {
        return productRepository.findAll();
    }

    public List<Product> getProductsByUser(String userEmail) {
        return productRepository.findByUserEmail(userEmail);
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
    public void deleteProduct(Long id, String userEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (userEmail != null && !userEmail.isBlank() && !userEmail.equals(product.getUserEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        priceEntryRepository.deleteByProductId(id);
        productRepository.deleteById(id);
        log.info("Deleted product id={}", id);
    }

    public List<Product> searchByTitle(String query) {
        return productRepository.findByTitleContainingIgnoreCase(query);
    }
}