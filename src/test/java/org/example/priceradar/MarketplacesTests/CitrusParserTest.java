package org.example.priceradar.MarketplacesTests;

import org.example.priceradar.marketplace.CitrusParser;
import org.example.priceradar.model.ProductCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CitrusParserTest {

    private final CitrusParser citrusParser = new CitrusParser();

    private static final String MOCK_JSON = """
            {
                "query": "Apple iPhone 17e 256GB Black",
                "total": 1,
                "results": {
                    "suggest": [],
                    "categories": [
                        {
                            "id": "20",
                            "name": "\\u0421\\u043c\\u0430\\u0440\\u0442\\u0444\\u043e\\u043d\\u0438",
                            "count": 1
                        }
                    ],
                    "items": [
                        {
                            "id": "789804",
                            "oldprice": "37999",
                            "url": "https://www.ctrs.com.ua/smartfony/smartfon-apple-iphone-17e-256gb-black-789804.html",
                            "is_presence": true,
                            "name": "\\u0421\\u043c\\u0430\\u0440\\u0442\\u0444\\u043e\\u043d Apple iPhone 17e 256GB Black",
                            "brand": "Apple",
                            "price": "34999",
                            "currency": "\\u0433\\u0440\\u043d",
                            "discount_percent": "8"
                        }
                    ]
                }
            }
            """;

    @Test
    void parseProducts_shouldReturnOneProduct() {
        List<ProductCandidate> products = citrusParser.parseProducts(MOCK_JSON);

        assertEquals(1, products.size());
    }

    @Test
    void parseProducts_shouldHaveCorrectMarketplaceName() {
        List<ProductCandidate> products = citrusParser.parseProducts(MOCK_JSON);

        assertEquals("Citrus", products.get(0).marketplace());
    }

    @Test
    void parseProducts_shouldDecodeUnicodeName() {
        List<ProductCandidate> products = citrusParser.parseProducts(MOCK_JSON);

        assertEquals(
                "Смартфон Apple iPhone 17e 256GB Black",
                products.get(0).title()
        );
    }

    @Test
    void parseProducts_shouldHaveCorrectPrice() {
        List<ProductCandidate> products = citrusParser.parseProducts(MOCK_JSON);

        assertEquals(34999L, products.get(0).price());
    }

    @Test
    void parseProducts_shouldHaveCorrectUrl() {
        List<ProductCandidate> products = citrusParser.parseProducts(MOCK_JSON);

        assertEquals(
                "https://www.ctrs.com.ua/smartfony/smartfon-apple-iphone-17e-256gb-black-789804.html",
                products.get(0).url()
        );
    }

    @Test
    void parseProducts_shouldHaveCorrectPresence() {
        List<ProductCandidate> products = citrusParser.parseProducts(MOCK_JSON);

        assertTrue(products.get(0).inStock());
    }

    @Test
    void parseProducts_emptyItems_shouldReturnEmptyList() {
        String emptyJson = """
                {
                    "results": {
                        "items": []
                    }
                }
                """;

        List<ProductCandidate> products = citrusParser.parseProducts(emptyJson);

        assertTrue(products.isEmpty());
    }
}