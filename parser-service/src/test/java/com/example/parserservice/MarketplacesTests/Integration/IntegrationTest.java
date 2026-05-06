package com.example.parserservice.MarketplacesTests.Integration;


import com.example.parserservice.marketplace.CitrusParser;
import com.example.parserservice.marketplace.OpenShopParser;
import com.example.parserservice.marketplace.StoreInUAParser;
import com.example.parserservice.model.ProductCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class IntegrationTest {

    @Autowired
    private CitrusParser citrusParser;

    @Autowired
    private StoreInUAParser storeInUAParser;

    @Autowired
    private OpenShopParser openShopParser;

    // ---- CitrusParser ----

    @Test
    void citrus_shouldReturnProductsForValidQuery() {
        List<ProductCandidate> result = citrusParser.searchProducts("iPhone 17e");

        assertFalse(result.isEmpty());
    }

    @Test
    void citrus_allProducts_shouldHavePositivePrice() {
        List<ProductCandidate> result = citrusParser.searchProducts("iPhone 17e");

        assertTrue(result.stream().allMatch(p -> p.price() > 0));
    }

    @Test
    void citrus_allProducts_shouldHaveMarketplaceName() {
        List<ProductCandidate> result = citrusParser.searchProducts("iPhone 17e");

        assertTrue(result.stream().allMatch(p -> p.marketplace().equals("Citrus")));
    }

    @Test
    void citrus_allProducts_shouldHaveNonEmptyUrl() {
        List<ProductCandidate> result = citrusParser.searchProducts("iPhone 17e");

        assertTrue(result.stream().allMatch(p -> !p.url().isBlank()));
    }

    @Test
    void citrus_emptyQuery_shouldReturnEmptyList() {
        List<ProductCandidate> result = citrusParser.searchProducts("xzxzxzxzxz_не_існує");

        assertTrue(result.isEmpty());
    }

    // ---- StoreInUAParser ----

    @Test
    void storeInUA_shouldReturnProductsForValidQuery() {
        List<ProductCandidate> result = storeInUAParser.searchProducts("iPhone 17e");

        assertFalse(result.isEmpty());
    }

    @Test
    void storeInUA_allProducts_shouldHavePositivePrice() {
        List<ProductCandidate> result = storeInUAParser.searchProducts("iPhone 17e");

        assertTrue(result.stream().allMatch(p -> p.price() > 0));
    }

    @Test
    void storeInUA_allProducts_shouldHaveMarketplaceName() {
        List<ProductCandidate> result = storeInUAParser.searchProducts("iPhone 17e");

        assertTrue(result.stream().allMatch(p -> !p.marketplace().isBlank()));
    }

    // ---- OpenShopParser ----

    @Test
    void openShop_shouldReturnProductsForValidQuery() {
        List<ProductCandidate> result = openShopParser.searchProducts("iPhone 17e");

        assertFalse(result.isEmpty());
    }

    @Test
    void openShop_allProducts_shouldHavePositivePrice() {
        List<ProductCandidate> result = openShopParser.searchProducts("iPhone 17e");

        assertTrue(result.stream().allMatch(p -> p.price() > 0));
    }

    @Test
    void openShop_allProducts_shouldHaveNonEmptyUrl() {
        List<ProductCandidate> result = openShopParser.searchProducts("iPhone 17e");

        assertTrue(result.stream().allMatch(p -> !p.url().isBlank()));
    }
}