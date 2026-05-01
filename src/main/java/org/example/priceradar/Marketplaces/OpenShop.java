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

public class OpenShop implements MarketplaceSearchParser {

    private static final CloseableHttpClient client = HttpClients.createDefault();

    @Override
    public String marketplaceName() {
        return "OpenShop";
    }

    private String cleanPrice(String rawPrice) {
        return rawPrice
                .replaceAll("<[^>]*>", "")
                .replace("грн.", "")
                .trim();
    }

    public List<ProductCandidate> parseProducts(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        List<ProductCandidate> products = new ArrayList<>();

        for (JsonNode item : root) {
            products.add(new ProductCandidate(
                    marketplaceName(),
                    item.path("name").asText(),
                    Long.parseLong(cleanPrice(item.path("special").asText()).replace(".00", "")),
                    item.path("href").asText(),
                    true
            ));
        }

        return products;
    }

    @Override
    public List<ProductCandidate> searchProducts(String productName) {
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

        } catch (IOException e) {
            throw new RuntimeException("HTTP request failed", e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        OpenShop openShop = new OpenShop();

        List<ProductCandidate> products = openShop.searchProducts("Victus");

        products.forEach(p ->
                System.out.println(p.title() + " | " + p.price() + " грн | " + p.url())
        );
    }
}