package com.example.parserservice.MarketplacesTests.Unit;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.example.priceradar.marketplace.OpenShopParser;
import org.example.priceradar.model.ProductCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenShopParserTest {

    private final OpenShopParser openShopParser = new OpenShopParser(HttpClients.createDefault());

    private static final String MOCK_JSON = """
            [
                {
                    "product_id": "773146",
                    "name": "\\u0421\\u043c\\u0430\\u0440\\u0442\\u0444\\u043e\\u043d Apple iPhone 17e 256GB Black (MHRV4)",
                    "price": "37322.64<span class=\\"cs-currency\\"> \\u0433\\u0440\\u043d.<\\/span>",
                    "special": "33624.00<span class=\\"cs-currency\\"> \\u0433\\u0440\\u043d.<\\/span>",
                    "href": "https://openshop.ua/smartfon-apple-iphone-17e-256gb-black-mhrv4/"
                },
                {
                    "product_id": "773147",
                    "name": "\\u0421\\u043c\\u0430\\u0440\\u0442\\u0444\\u043e\\u043d Apple iPhone 17e 256GB eSIM Black (NHRN4)",
                    "price": "35321.31<span class=\\"cs-currency\\"> \\u0433\\u0440\\u043d.<\\/span>",
                    "special": "31821.00<span class=\\"cs-currency\\"> \\u0433\\u0440\\u043d.<\\/span>",
                    "href": "https://openshop.ua/smartfon-apple-iphone-17e-256gb-esim-black-nhrn4/"
                }
            ]
            """;

    @Test
    void parseProducts_shouldReturnTwoProducts() {
        List<ProductCandidate> products = openShopParser.parseProducts(MOCK_JSON);

        assertEquals(2, products.size());
    }

    @Test
    void parseProducts_firstProduct_shouldHaveCorrectMarketplace() {
        List<ProductCandidate> products = openShopParser.parseProducts(MOCK_JSON);

        assertEquals("OpenShop", products.get(0).marketplace());
    }

    @Test
    void parseProducts_firstProduct_shouldDecodeUnicodeName() {
        List<ProductCandidate> products = openShopParser.parseProducts(MOCK_JSON);

        assertEquals(
                "Смартфон Apple iPhone 17e 256GB Black (MHRV4)",
                products.get(0).title()
        );
    }

    @Test
    void parseProducts_firstProduct_shouldCleanPriceFromHtml() {
        List<ProductCandidate> products = openShopParser.parseProducts(MOCK_JSON);

        assertEquals(33624L, products.get(0).price());
    }

    @Test
    void parseProducts_firstProduct_shouldHaveCorrectUrl() {
        List<ProductCandidate> products = openShopParser.parseProducts(MOCK_JSON);

        assertEquals(
                "https://openshop.ua/smartfon-apple-iphone-17e-256gb-black-mhrv4/",
                products.get(0).url()
        );
    }

    @Test
    void parseProducts_firstProduct_shouldBeAvailable() {
        List<ProductCandidate> products = openShopParser.parseProducts(MOCK_JSON);

        assertTrue(products.get(0).inStock());
    }

    @Test
    void parseProducts_secondProduct_shouldHaveCorrectPrice() {
        List<ProductCandidate> products = openShopParser.parseProducts(MOCK_JSON);

        assertEquals(31821L, products.get(1).price());
    }

    @Test
    void parseProducts_secondProduct_shouldDecodeUnicodeName() {
        List<ProductCandidate> products = openShopParser.parseProducts(MOCK_JSON);

        assertEquals(
                "Смартфон Apple iPhone 17e 256GB eSIM Black (NHRN4)",
                products.get(1).title()
        );
    }

    @Test
    void parseProducts_emptyArray_shouldReturnEmptyList() {
        List<ProductCandidate> products = openShopParser.parseProducts("[]");

        assertTrue(products.isEmpty());
    }
}