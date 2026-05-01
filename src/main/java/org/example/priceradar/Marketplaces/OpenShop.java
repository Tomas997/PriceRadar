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

public class OpenShop {

    private static final CloseableHttpClient client = HttpClients.createDefault();

    // Очищає ціну від HTML тегів: "33624.00<span...> грн.</span>" → "33624.00"
    private static String cleanPrice(String rawPrice) {
        return rawPrice.replaceAll("<[^>]*>", "").replace("грн.", "").trim();
    }

    public static List<Map<String, String>> parseProducts(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        List<Map<String, String>> products = new ArrayList<>();

        for (JsonNode item : root) {
            Map<String, String> product = new HashMap<>();
            product.put("id",    item.path("product_id").asText());
            product.put("name",  item.path("name").asText()); // Jackson декодує \\uXXXX автоматично
            product.put("price", cleanPrice(item.path("special").asText())); // акційна ціна
            product.put("url",   item.path("href").asText());
            products.add(product);
        }

        return products;
    }

    public static List<Map<String, String>> searchProducts(String productName) throws Exception {
        String encodedName = URLEncoder.encode(productName, StandardCharsets.UTF_8);
        String url = "https://openshop.ua/index.php" +
                "?route=extension/module/cyber_autosearch/ajaxLiveSearch" +
                "&filter_name=" + encodedName;

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
        List<Map<String, String>> products = searchProducts("Victus");
        products.forEach(p ->
                System.out.println(p.get("name") + " | " + p.get("price") + " грн | " + p.get("url"))
        );
    }
}