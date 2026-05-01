package org.example.priceradar.MarketplacesTests;

import org.example.priceradar.Marketplaces.StoreInUA;
import org.example.priceradar.model.ProductCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreInUATest {

    private final StoreInUA storeInUA = new StoreInUA();

    private static final String MOCK_JSON = """
            {
                "data": [
                    {
                        "id": 145716,
                        "slug": "apple-iphone-17e-256gb-black",
                        "title": "Apple iPhone 17e 256GB Black (MHRV4)",
                        "sku": "ST-145716",
                        "quantity": 1,
                        "prices": {
                            "price": "33364.00",
                            "old_price": 0,
                            "discount_price": null
                        }
                    },
                    {
                        "id": 145722,
                        "slug": "apple-iphone-17e-256gb-esim-black",
                        "title": "Apple iPhone 17e 256GB eSim Black (NHRN4)",
                        "sku": "ST-145722",
                        "quantity": 0,
                        "prices": {
                            "price": "29999.00",
                            "old_price": 0,
                            "discount_price": null
                        }
                    }
                ]
            }
            """;

    @Test
    void parseProducts_shouldReturnCorrectSize() {
        List<ProductCandidate> products = storeInUA.parseProducts(MOCK_JSON);

        assertEquals(2, products.size());
    }

    @Test
    void parseProducts_firstProduct_shouldHaveCorrectMarketplace() {
        List<ProductCandidate> products = storeInUA.parseProducts(MOCK_JSON);

        assertEquals("StoreInUA", products.get(0).marketplace());
    }

    @Test
    void parseProducts_firstProduct_shouldHaveCorrectTitle() {
        List<ProductCandidate> products = storeInUA.parseProducts(MOCK_JSON);

        assertEquals("Apple iPhone 17e 256GB Black (MHRV4)", products.get(0).title());
    }

    @Test
    void parseProducts_firstProduct_shouldHaveCorrectPrice() {
        List<ProductCandidate> products = storeInUA.parseProducts(MOCK_JSON);

        assertEquals(33364L, products.get(0).price());
    }

    @Test
    void parseProducts_firstProduct_shouldHaveCorrectUrl() {
        List<ProductCandidate> products = storeInUA.parseProducts(MOCK_JSON);

        assertEquals(
                "https://storeinua.com/products/apple-iphone-17e-256gb-black",
                products.get(0).url()
        );
    }

    @Test
    void parseProducts_firstProduct_shouldBeAvailable() {
        List<ProductCandidate> products = storeInUA.parseProducts(MOCK_JSON);

        assertTrue(products.get(0).inStock());
    }

    @Test
    void parseProducts_secondProduct_shouldHaveCorrectPrice() {
        List<ProductCandidate> products = storeInUA.parseProducts(MOCK_JSON);

        assertEquals(29999L, products.get(1).price());
    }

    @Test
    void parseProducts_secondProduct_shouldNotBeAvailable() {
        List<ProductCandidate> products = storeInUA.parseProducts(MOCK_JSON);

        assertFalse(products.get(1).inStock());
    }

    @Test
    void parseProducts_emptyData_shouldReturnEmptyList() {
        String emptyJson = "{\"data\": []}";

        List<ProductCandidate> products = storeInUA.parseProducts(emptyJson);

        assertTrue(products.isEmpty());
    }
}