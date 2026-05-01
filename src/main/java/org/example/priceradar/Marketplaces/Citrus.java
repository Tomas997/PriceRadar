package org.example.priceradar.Marketplaces;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Citrus {

    private static final CloseableHttpClient client = HttpClients.createDefault();

    public static List<Map<String, String>> parseProducts(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode items = root.path("results").path("items");

        List<Map<String, String>> products = new ArrayList<>();

        for (JsonNode item : items) {
            Map<String, String> product = new HashMap<>();
            // Jackson автоматично декодує \\uXXXX послідовності
            product.put("id",    item.path("id").asText());
            product.put("name",  item.path("name").asText());
            product.put("price", item.path("price").asText());
            product.put("url",   item.path("url").asText());
            products.add(product);
        }

        return products;
    }

    public static List<Map<String, String>> searchProducts(String productName) throws Exception {
        String encodedName = URLEncoder.encode(productName, StandardCharsets.UTF_8);
        String url = "https://api.ctrs.com.ua/catalog/api/autocomplete-search" +
                "?autocomplete=true&l=uk&query=" + encodedName;

        HttpGet request = new HttpGet(url);
        request.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = client.execute(request)) {
            if (response.getCode() != 200) {
                throw new RuntimeException("HTTP помилка: " + response.getCode());
            }
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return parseProducts(body);
        }
    }

    public static void main(String[] args) throws Exception {
        List<Map<String, String>> products = searchProducts("Watch8");
        products.forEach(p ->
                System.out.println(p.get("name") + " | " + p.get("price") + " грн | " + p.get("url"))
        );
    }
}