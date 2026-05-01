package org.example.priceradar.Marketplaces;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoreInUA {
    private static final BasicCookieStore cookieStore = new BasicCookieStore();

    private static final CloseableHttpClient client = HttpClients.custom()
            .setDefaultCookieStore(cookieStore)
            .build();


    public static List<Map<String, String>> parseProducts(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode data = root.path("data");

        List<Map<String, String>> products = new ArrayList<>();

        for (JsonNode item : data) {
            Map<String, String> product = new HashMap<>();
            product.put("slug",  item.path("slug").asText());
            product.put("title", item.path("title").asText());
            product.put("sku",   item.path("sku").asText());
            product.put("price", item.path("prices").path("price").asText());
            products.add(product);
        }

        return products;
    }

    public static List<Map<String, String>> searchProducts(String productName)  {
        String encodedName = URLEncoder.encode(productName, StandardCharsets.UTF_8);
        String url = "https://api.storeinua.com/api/promo/search/?query=" + encodedName;

        HttpGet request = new HttpGet(url);
        request.addHeader("Accept", "application/json");
        request.addHeader("Content-type", "application/json");

        try (CloseableHttpResponse response = client.execute(request)) {
            if (response.getCode() != 200) {
                throw new RuntimeException("HTTP помилка: " + response.getCode());
            }
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return parseProducts(body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        String productName = "Optonica";
        List<Map<String, String>> products = searchProducts(productName);
        products.forEach(p -> System.out.println(p.get("title") + " | " + p.get("price")));
    }
}
