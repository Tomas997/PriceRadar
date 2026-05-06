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
public class CitrusParser implements MarketplaceSearchParser {

    private final ObjectMapper mapper;
    private final CloseableHttpClient httpClient;

    @Override
    public String marketplaceName() {
        return "Citrus";
    }

    @Override
    public List<ProductCandidate> parseProducts(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode items = root.path("results").path("items");

            List<ProductCandidate> products = new ArrayList<>();
            for (JsonNode item : items) {
                products.add(new ProductCandidate(
                        marketplaceName(),
                        item.path("name").asText(),
                        parsePrice(item.path("price").asText()),
                        item.path("url").asText(),
                        item.path("is_presence").asBoolean()
                ));
            }
            return products;
        } catch (Exception e) {
            throw new ProductParseException("Failed to parse Citrus response", e);
        }
    }

    @Override
    public List<ProductCandidate> searchProducts(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://api.ctrs.com.ua/catalog/api/autocomplete-search"
                + "?autocomplete=true&l=uk&query=" + encoded;

        HttpGet request = new HttpGet(url);
        request.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() != 200) {
                throw new ProductParseException("Citrus HTTP " + response.getCode(), null);
            }
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return parseProducts(body);
        } catch (IOException | ParseException e) {
            throw new ProductParseException("Citrus request failed", e);
        }
    }

    private long parsePrice(String raw) {
        try {
            return (long) Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("Cannot parse Citrus price: '{}'", raw);
            return 0L;
        }
    }
}
