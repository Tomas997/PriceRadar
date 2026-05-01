package org.example.priceradar.Marketplaces;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.example.priceradar.model.ProductCandidate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Citrus implements MarketplaceSearchParser {

    private static final CloseableHttpClient client = HttpClients.createDefault();

    @Override
    public String marketplaceName() {
        return "Citrus";
    }


    public List<ProductCandidate> parseProducts(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode items = root.path("results").path("items");

        List<ProductCandidate> products = new ArrayList<>();

        for (JsonNode item : items) {
            products.add(new ProductCandidate(
                    marketplaceName(),
                    item.path("name").asText(),
                    Long.parseLong(item.path("price").asText()),
                    item.path("url").asText(),
                    item.path("is_presence").asBoolean()
            ));
        }

        return products;
    }

    @Override
    public List<ProductCandidate> searchProducts(String productName) {
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

        } catch (IOException e) {
            throw new RuntimeException("HTTP request failed", e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        Citrus citrus = new Citrus();
        List<ProductCandidate> products = citrus.searchProducts("Apple iPhone 17e 256GB Black");
        products.forEach(p ->
                System.out.println(p.title() + " | " + p.price() + " грн | " + p.url())
        );
    }
}