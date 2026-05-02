package org.example.priceradar.marketplace;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.example.priceradar.model.ProductCandidate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


@Component
public class StoreInUAParser implements MarketplaceSearchParser {
    private final ObjectMapper mapper = new ObjectMapper();

    private final CloseableHttpClient client;

    public StoreInUAParser(CloseableHttpClient client) {
        this.client = client;
    }

    @Override
    public String marketplaceName() {
        return "StoreInUA";
    }

    @Override
    public List<ProductCandidate> parseProducts(String json) {
        JsonNode root = mapper.readTree(json);
        JsonNode data = root.path("data");

        List<ProductCandidate> products = new ArrayList<>();

        for (JsonNode item : data) {
            products.add(new ProductCandidate(
                    marketplaceName(),
                    item.path("title").asText(),
                    Long.parseLong(item.path("prices").path("price").asText().replace(".00", "")),
                    "https://storeinua.com/products/" + item.path("slug").asText(),
                    item.path("quantity").asInt() > 0
            ));
        }

        return products;
    }

    @Override
    public List<ProductCandidate> searchProducts(String productName) {
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

//    public static void main(String[] args) {
//        StoreInUAParser storeInUaParser = new StoreInUAParser(HttpClients.createDefault());
//
//        List<ProductCandidate> products = storeInUaParser.searchProducts("Optonica");
//
//        products.forEach(p ->
//                System.out.println(p.title() + " | " + p.price() + " грн | " + p.url())
//        );
//    }
}