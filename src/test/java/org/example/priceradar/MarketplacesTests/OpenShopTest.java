package org.example.priceradar.MarketplacesTests;

import org.example.priceradar.Marketplaces.OpenShop;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenShopTest {

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
    void parseProducts_shouldReturnTwoProducts() throws Exception {
        List<Map<String, String>> products = OpenShop.parseProducts(MOCK_JSON);
        assertEquals(2, products.size());
    }

    @Test
    void parseProducts_firstProduct_shouldDecodeUnicodeName() throws Exception {
        List<Map<String, String>> products = OpenShop.parseProducts(MOCK_JSON);
        assertEquals("Смартфон Apple iPhone 17e 256GB Black (MHRV4)", products.get(0).get("name"));
    }

    @Test
    void parseProducts_firstProduct_shouldCleanPriceFromHtml() throws Exception {
        List<Map<String, String>> products = OpenShop.parseProducts(MOCK_JSON);
        assertEquals("33624.00", products.get(0).get("price"));
    }

    @Test
    void parseProducts_firstProduct_shouldHaveCorrectId() throws Exception {
        List<Map<String, String>> products = OpenShop.parseProducts(MOCK_JSON);
        assertEquals("773146", products.get(0).get("id"));
    }

    @Test
    void parseProducts_firstProduct_shouldHaveCorrectUrl() throws Exception {
        List<Map<String, String>> products = OpenShop.parseProducts(MOCK_JSON);
        assertEquals(
                "https://openshop.ua/smartfon-apple-iphone-17e-256gb-black-mhrv4/",
                products.get(0).get("url")
        );
    }

    @Test
    void parseProducts_secondProduct_shouldHaveCorrectPrice() throws Exception {
        List<Map<String, String>> products = OpenShop.parseProducts(MOCK_JSON);
        assertEquals("31821.00", products.get(1).get("price"));
    }

    @Test
    void parseProducts_secondProduct_shouldDecodeUnicodeName() throws Exception {
        List<Map<String, String>> products = OpenShop.parseProducts(MOCK_JSON);
        assertEquals("Смартфон Apple iPhone 17e 256GB eSIM Black (NHRN4)", products.get(1).get("name"));
    }

    @Test
    void parseProducts_emptyArray_shouldReturnEmptyList() throws Exception {
        List<Map<String, String>> products = OpenShop.parseProducts("[]");
        assertTrue(products.isEmpty());
    }
}