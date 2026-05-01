package org.example.priceradar.MarketplacesTests;

import org.example.priceradar.Marketplaces.StoreInUA;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StoreInUATest {

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
        List<Map<String, String>> products = StoreInUA.parseProducts(MOCK_JSON);
        assertEquals(2, products.size());
    }

    @Test
    void parseProducts_firstProduct_shouldHaveCorrectSlug() {
        List<Map<String, String>> products = StoreInUA.parseProducts(MOCK_JSON);
        assertEquals("apple-iphone-17e-256gb-black", products.get(0).get("slug"));
    }

    @Test
    void parseProducts_firstProduct_shouldHaveCorrectTitle() {
        List<Map<String, String>> products = StoreInUA.parseProducts(MOCK_JSON);
        assertEquals("Apple iPhone 17e 256GB Black (MHRV4)", products.get(0).get("title"));
    }

    @Test
    void parseProducts_firstProduct_shouldHaveCorrectSku() {
        List<Map<String, String>> products = StoreInUA.parseProducts(MOCK_JSON);
        assertEquals("ST-145716", products.get(0).get("sku"));
    }

    @Test
    void parseProducts_firstProduct_shouldHaveCorrectPrice() {
        List<Map<String, String>> products = StoreInUA.parseProducts(MOCK_JSON);
        assertEquals("33364.00", products.get(0).get("price"));
    }

    @Test
    void parseProducts_secondProduct_shouldHaveCorrectPrice() {
        List<Map<String, String>> products = StoreInUA.parseProducts(MOCK_JSON);
        assertEquals("29999.00", products.get(1).get("price"));
    }

    @Test
    void parseProducts_emptyData_shouldReturnEmptyList() {
        String emptyJson = "{\"data\": []}";
        List<Map<String, String>> products = StoreInUA.parseProducts(emptyJson);
        assertTrue(products.isEmpty());
    }
}