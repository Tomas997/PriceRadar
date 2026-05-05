package org.example.priceradar.service;

import org.example.priceradar.marketplace.MarketplaceSearchParser;
import org.example.priceradar.dto.MarketplaceSearchResult;
import org.example.priceradar.model.PriceEntry;
import org.example.priceradar.model.Product;
import org.example.priceradar.model.ProductCandidate;
import org.example.priceradar.repository.PriceEntryRepository;
import org.example.priceradar.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductSearchService {

    private final List<MarketplaceSearchParser> parsers;
    private final ProductRepository productRepository;
    private final PriceEntryRepository priceEntryRepository;

    public ProductSearchService(List<MarketplaceSearchParser> parsers,
                                ProductRepository productRepository,
                                PriceEntryRepository priceEntryRepository) {
        this.parsers = parsers;
        this.productRepository = productRepository;
        this.priceEntryRepository = priceEntryRepository;
    }

    public List<MarketplaceSearchResult> search(String query) {
        return parsers.parallelStream()
                .map(parser -> {
                    try {
                        return new MarketplaceSearchResult(
                                parser.marketplaceName(),
                                parser.searchProducts(query)
                        );
                    } catch (Exception e) {
                        log.error("Parser {} failed", parser.marketplaceName(), e);
                        return new MarketplaceSearchResult(parser.marketplaceName(), List.of());
                    }
                })
                .toList();
    }

    public Product trackProduct(ProductCandidate candidate) {
        Product product = productRepository.save(new Product(
                candidate.title(),
                candidate.marketplace(),
                candidate.url(),
                candidate.inStock()
        ));

        priceEntryRepository.save(new PriceEntry(product.getId(), candidate.price()));

        return product;
    }
}