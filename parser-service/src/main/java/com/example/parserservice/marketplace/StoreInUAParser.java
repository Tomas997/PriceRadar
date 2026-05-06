package com.example.parserservice.marketplace;

import com.example.parserservice.exception.ProductParseException;
import com.example.parserservice.model.ProductCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreInUAParser implements MarketplaceSearchParser {

    private final ObjectMapper mapper;
    private final CloseableHttpClient httpClient;

    @Override
    public String marketplaceName() {
        return "StoreInUA";
    }

    @Override
    public List<ProductCandidate> parseProducts(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode data = root.path("data");

            List<ProductCandidate> products = new ArrayList<>();
            for (JsonNode item : data) {
                products.add(new ProductCandidate(
                        marketplaceName(),
                        item.path("title").asText(),
                        parsePrice(item.path("prices").path("price").asText()),
                        "https://storeinua.com/products/" + item.path("slug").asText(),
                        item.path("quantity").asInt() > 0
                ));
            }
            return products;
        } catch (Exception e) {
            throw new ProductParseException("Failed to parse StoreInUA response", e);
        }
    }

    @Override
    public List<ProductCandidate> searchProducts(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://api.storeinua.com/api/promo/search/?query=" + encoded;

        HttpGet request = new HttpGet(url);
        request.addHeader("Accept", "application/json");
        request.addHeader("Content-Type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() != 200) {
                throw new ProductParseException("StoreInUA HTTP " + response.getCode(), null);
            }
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return parseProducts(body);
        } catch (IOException | ParseException e) {
            throw new ProductParseException("StoreInUA request failed", e);
        }
    }

    private long parsePrice(String raw) {
        try {
            return (long) Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("Cannot parse StoreInUA price: '{}'", raw);
            return 0L;
        }
    }
}
